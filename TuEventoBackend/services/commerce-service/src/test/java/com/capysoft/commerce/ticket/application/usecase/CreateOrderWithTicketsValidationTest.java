package com.capysoft.commerce.ticket.application.usecase;

import com.capysoft.commerce.ticket.application.dto.request.CreateOrderRequest;
import com.capysoft.commerce.ticket.application.dto.response.OrderResponse;
import com.capysoft.commerce.ticket.application.port.out.CatalogSeatsPort;
import com.capysoft.commerce.ticket.application.port.out.CatalogSeatsPort.HeldSeat;
import com.capysoft.commerce.ticket.application.port.out.CatalogSeatsPort.HeldSeats;
import com.capysoft.commerce.ticket.domain.model.Order;
import com.capysoft.commerce.ticket.domain.model.OrderStatus;
import com.capysoft.commerce.ticket.domain.model.SeatTicket;
import com.capysoft.commerce.ticket.domain.model.Ticket;
import com.capysoft.commerce.ticket.domain.repository.OrderRepository;
import com.capysoft.commerce.ticket.domain.repository.SeatTicketRepository;
import com.capysoft.commerce.ticket.domain.repository.TicketLogRepository;
import com.capysoft.commerce.ticket.domain.repository.TicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Crear una orden: las sillas se bloquean en Catálogo a nombre de la orden y sus precios
 * fijan el total y el de cada entrada. Si el bloqueo falla, no se crea ninguna entrada (y la
 * transacción deshace la orden en DRAFT). Las validaciones de cada silla: CatalogSeatsAdapterTest.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateOrderWithTicketsUseCaseImpl — sillas bloqueadas en Catálogo")
class CreateOrderWithTicketsValidationTest {

    private static final long USER_ID = 7L;
    private static final long EVENT_ID = 5L;
    private static final long ORDER_ID = 40L;

    @Mock OrderRepository orderRepository;
    @Mock TicketRepository ticketRepository;
    @Mock SeatTicketRepository seatTicketRepository;
    @Mock TicketLogRepository ticketLogRepository;
    @Mock CatalogSeatsPort catalogSeatsPort;

    @InjectMocks CreateOrderWithTicketsUseCaseImpl useCase;

    /** El repositorio devuelve la orden con su id, como la BD. */
    private void ordersGetAnId() {
        when(orderRepository.save(any())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            return Order.builder().orderId(ORDER_ID).userId(o.getUserId()).eventId(o.getEventId())
                    .totalAmount(o.getTotalAmount()).status(o.getStatus()).build();
        });
    }

    @Test
    @DisplayName("si Catálogo rechaza una silla, el error sube y no se crea ninguna entrada")
    void holdFailure_createsNoTickets() {
        ordersGetAnId();
        when(catalogSeatsPort.holdForOrder(eq(ORDER_ID), eq(USER_ID), eq(EVENT_ID), anyList()))
                .thenThrow(new IllegalStateException("Seat 10 is not reserved (status SOLD)"));

        assertThatThrownBy(() -> useCase.execute(new CreateOrderRequest(EVENT_ID, List.of(10)), USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is not reserved");
        verify(orderRepository, times(1)).save(any());
        verify(ticketRepository, never()).saveAll(any());
        verify(seatTicketRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("bloquea las sillas sin repetidas y en orden ascendente, a nombre de la orden")
    void holdsDistinctSortedSeatsForTheOrder() {
        ordersGetAnId();
        when(catalogSeatsPort.holdForOrder(eq(ORDER_ID), eq(USER_ID), eq(EVENT_ID), anyList()))
                .thenThrow(new IllegalStateException("stop"));

        assertThatThrownBy(() -> useCase.execute(new CreateOrderRequest(EVENT_ID, List.of(12, 10, 12)), USER_ID))
                .isInstanceOf(IllegalStateException.class);
        verify(catalogSeatsPort).holdForOrder(ORDER_ID, USER_ID, EVENT_ID, List.of(10, 12));
    }

    @Test
    @DisplayName("el total y cada entrada llevan el precio que devuelve Catálogo")
    void pricesComeFromTheHold() {
        ordersGetAnId();
        when(catalogSeatsPort.holdForOrder(ORDER_ID, USER_ID, EVENT_ID, List.of(10, 11))).thenReturn(new HeldSeats("COP",
                List.of(new HeldSeat(10, 70, new BigDecimal("100")), new HeldSeat(11, 71, new BigDecimal("250")))));
        AtomicLong ids = new AtomicLong(100);
        when(ticketRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<Ticket> saved = new ArrayList<>();
            for (Ticket t : inv.<List<Ticket>>getArgument(0)) {
                saved.add(Ticket.builder().ticketId(ids.getAndIncrement()).orderId(t.getOrderId())
                        .eventId(t.getEventId()).userId(t.getUserId()).status(t.getStatus())
                        .totalPrice(t.getTotalPrice()).build());
            }
            return saved;
        });

        OrderResponse response = useCase.execute(new CreateOrderRequest(EVENT_ID, List.of(11, 10)), USER_ID);

        assertThat(response.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("350");
        assertThat(response.getStatus()).isEqualTo(OrderStatus.DRAFT);
        assertThat(response.getTickets()).hasSize(2);

        ArgumentCaptor<List<SeatTicket>> seatTickets = ArgumentCaptor.captor();
        verify(seatTicketRepository).saveAll(seatTickets.capture());
        assertThat(seatTickets.getValue()).extracting(SeatTicket::getSeatId, SeatTicket::getTicketId, SeatTicket::getPrice)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(10, 100L, new BigDecimal("100")),
                        org.assertj.core.groups.Tuple.tuple(11, 101L, new BigDecimal("250")));
        verify(orderRepository, times(2)).save(any());
    }
}
