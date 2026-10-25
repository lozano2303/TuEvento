package com.capysoft.tuevento.modules.ticket.application.usecase;

import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.seat.application.port.in.SeatUseCase;
import com.capysoft.tuevento.modules.ticket.domain.model.Money;
import com.capysoft.tuevento.modules.ticket.domain.model.Order;
import com.capysoft.tuevento.modules.ticket.domain.model.OrderStatus;
import com.capysoft.tuevento.modules.ticket.domain.model.Ticket;
import com.capysoft.tuevento.modules.ticket.domain.model.TicketCheckin;
import com.capysoft.tuevento.modules.ticket.domain.model.TicketStatus;
import com.capysoft.tuevento.modules.ticket.domain.repository.OrderRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.SeatTicketRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketCheckinRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketLogRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Las órdenes y tickets solo los ve o cancela su comprador, y el check-in manual
 * solo lo hace el organizador dueño del evento (el validador es el autenticado).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Order and ticket ownership")
class OrderTicketOwnershipTest {

    private static final Long BUYER_ID = 7L;
    private static final Long OTHER_USER_ID = 8L;
    private static final Long ORGANIZER_ID = 30L;
    private static final Long ORDER_ID = 100L;
    private static final Long TICKET_ID = 200L;
    private static final Long EVENT_ID = 300L;

    @Mock private OrderRepository orderRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private SeatTicketRepository seatTicketRepository;
    @Mock private TicketLogRepository ticketLogRepository;
    @Mock private TicketCheckinRepository ticketCheckinRepository;
    @Mock private SeatUseCase seatUseCase;
    @Mock private EventRepository eventRepository;

    @InjectMocks private GetOrderUseCaseImpl getOrder;
    @InjectMocks private GetOrderTicketsUseCaseImpl getOrderTickets;
    @InjectMocks private CancelOrderUseCaseImpl cancelOrder;
    @InjectMocks private GetTicketUseCaseImpl getTicket;
    @InjectMocks private GetUserTicketsUseCaseImpl getUserTickets;
    @InjectMocks private CheckinTicketUseCaseImpl checkinTicket;

    private static Order order() {
        return Order.builder().orderId(ORDER_ID).userId(BUYER_ID).eventId(EVENT_ID)
                .totalAmount(new Money(new BigDecimal("50000"), "COP"))
                .status(OrderStatus.PAYMENT_PENDING).build();
    }

    private static Ticket paidTicket() {
        return Ticket.builder().ticketId(TICKET_ID).userId(BUYER_ID).orderId(ORDER_ID).eventId(EVENT_ID)
                .status(TicketStatus.PAID).expirationDate(LocalDateTime.now().plusDays(1)).build();
    }

    @Nested
    @DisplayName("Orders")
    class Orders {

        @Test
        @DisplayName("GET order of another user → denied")
        void getOrder_otherUser_isDenied() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order()));

            assertThatThrownBy(() -> getOrder.execute(ORDER_ID, OTHER_USER_ID))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("GET own order → returned with its tickets")
        void getOrder_buyer_isAllowed() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order()));
            when(ticketRepository.findByOrderId(ORDER_ID)).thenReturn(List.of());

            assertThat(getOrder.execute(ORDER_ID, BUYER_ID)).isNotNull();
        }

        @Test
        @DisplayName("GET tickets of another user's order → denied")
        void getOrderTickets_otherUser_isDenied() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order()));

            assertThatThrownBy(() -> getOrderTickets.execute(ORDER_ID, OTHER_USER_ID))
                    .isInstanceOf(AccessDeniedException.class);
            verify(ticketRepository, never()).findByOrderId(any());
        }

        @Test
        @DisplayName("Cancel another user's order → denied and nothing is saved")
        void cancelOrder_otherUser_isDenied() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order()));

            assertThatThrownBy(() -> cancelOrder.execute(ORDER_ID, OTHER_USER_ID))
                    .isInstanceOf(AccessDeniedException.class);
            verify(orderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tickets")
    class Tickets {

        @Test
        @DisplayName("GET ticket of another user → denied")
        void getTicket_otherUser_isDenied() {
            when(ticketRepository.findById(TICKET_ID)).thenReturn(Optional.of(paidTicket()));

            assertThatThrownBy(() -> getTicket.execute(TICKET_ID, OTHER_USER_ID))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("GET tickets of another user → denied unless admin")
        void getUserTickets_onlySelfOrAdmin() {
            assertThatThrownBy(() -> getUserTickets.executeFor(BUYER_ID, OTHER_USER_ID, false))
                    .isInstanceOf(AccessDeniedException.class);

            when(ticketRepository.findByUserId(BUYER_ID)).thenReturn(List.of());
            assertThat(getUserTickets.executeFor(BUYER_ID, OTHER_USER_ID, true)).isEmpty();
            assertThat(getUserTickets.executeFor(BUYER_ID, BUYER_ID, false)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Manual check-in")
    class Checkin {

        @Test
        @DisplayName("Organizer of another event → denied and the ticket is not used")
        void checkin_notEventOwner_isDenied() {
            when(ticketRepository.findByIdForUpdate(TICKET_ID)).thenReturn(Optional.of(paidTicket()));
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(Event.builder().userId(ORGANIZER_ID).build()));

            assertThatThrownBy(() -> checkinTicket.execute(TICKET_ID, OTHER_USER_ID))
                    .isInstanceOf(AccessDeniedException.class);
            verify(ticketCheckinRepository, never()).save(any());
            verify(ticketRepository, never()).save(any());
        }

        @Test
        @DisplayName("Event owner → check-in recorded with the authenticated organizer as validator")
        void checkin_eventOwner_isRecorded() {
            when(ticketRepository.findByIdForUpdate(TICKET_ID)).thenReturn(Optional.of(paidTicket()));
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(Event.builder().userId(ORGANIZER_ID).build()));
            when(ticketCheckinRepository.existsByTicketId(TICKET_ID)).thenReturn(false);
            when(ticketCheckinRepository.save(any(TicketCheckin.class))).thenAnswer(inv -> inv.getArgument(0));

            var response = checkinTicket.execute(TICKET_ID, ORGANIZER_ID);

            assertThat(response.getValidatedBy()).isEqualTo(ORGANIZER_ID);
        }
    }
}
