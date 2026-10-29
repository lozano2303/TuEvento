package com.capysoft.tuevento.modules.ticket.application.usecase;

import com.capysoft.tuevento.modules.ticket.application.port.out.CatalogSeatsPort;
import com.capysoft.tuevento.modules.ticket.domain.model.Order;
import com.capysoft.tuevento.modules.ticket.domain.model.OrderStatus;
import com.capysoft.tuevento.modules.ticket.domain.model.SeatTicket;
import com.capysoft.tuevento.modules.ticket.domain.model.Ticket;
import com.capysoft.tuevento.modules.ticket.domain.model.TicketStatus;
import com.capysoft.tuevento.modules.ticket.domain.repository.OrderRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.SeatTicketRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketLogRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Al confirmarse el pago, cada silla se vende a nombre del comprador de la orden. Si alguna
 * ya la tiene otra persona, la confirmación falla en vez de entregar la misma silla dos veces.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConfirmOrderPaymentUseCaseImpl — venta de las sillas")
class ConfirmOrderPaymentUseCaseImplTest {

    private static final long ORDER_ID = 15L;
    private static final long BUYER_ID = 7L;

    @Mock OrderRepository orderRepository;
    @Mock TicketRepository ticketRepository;
    @Mock TicketLogRepository ticketLogRepository;
    @Mock SeatTicketRepository seatTicketRepository;
    @Mock CatalogSeatsPort catalogSeatsPort;

    @InjectMocks ConfirmOrderPaymentUseCaseImpl useCase;

    private Ticket ticket;

    @BeforeEach
    void setUp() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(Order.builder()
                .orderId(ORDER_ID).userId(BUYER_ID).eventId(5L).status(OrderStatus.PAYMENT_PENDING).build()));
        ticket = Ticket.builder().ticketId(100L).orderId(ORDER_ID).status(TicketStatus.PROCESSING).build();
        when(ticketRepository.findByOrderId(ORDER_ID)).thenReturn(List.of(ticket));
        when(seatTicketRepository.findByTicketId(100L)).thenReturn(List.of(
                SeatTicket.builder().seatId(10).ticketId(100L).build(),
                SeatTicket.builder().seatId(11).ticketId(100L).build()));
    }

    @Test
    @DisplayName("marca la orden y las entradas como pagadas y vende cada silla al comprador")
    void sellsEverySeatToTheBuyer() {
        useCase.confirmOrderPayment(ORDER_ID, "gw-1");

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.PAID);
        verify(catalogSeatsPort).sell(eq(ORDER_ID), eq(BUYER_ID), eq(List.of(10, 11)), anyString());
    }

    @Test
    @DisplayName("si una silla ya es de otra persona, la confirmación falla (y la transacción se deshace)")
    void failsWhenASeatWasTaken() {
        doThrow(new BusinessException("SEAT_ALREADY_TAKEN", "La silla 10 ya no está disponible"))
                .when(catalogSeatsPort).sell(eq(ORDER_ID), eq(BUYER_ID), eq(List.of(10, 11)), anyString());

        assertThatThrownBy(() -> useCase.confirmOrderPayment(ORDER_ID, "gw-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya no está disponible");
    }
}
