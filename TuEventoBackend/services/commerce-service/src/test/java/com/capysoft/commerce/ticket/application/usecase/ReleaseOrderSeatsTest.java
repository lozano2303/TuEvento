package com.capysoft.commerce.ticket.application.usecase;

import com.capysoft.commerce.ticket.application.port.out.CatalogSeatsPort;
import com.capysoft.commerce.ticket.domain.model.Money;
import com.capysoft.commerce.ticket.domain.model.Order;
import com.capysoft.commerce.ticket.domain.model.OrderStatus;
import com.capysoft.commerce.ticket.domain.model.SeatTicket;
import com.capysoft.commerce.ticket.domain.model.Ticket;
import com.capysoft.commerce.ticket.domain.model.TicketStatus;
import com.capysoft.commerce.ticket.domain.repository.OrderRepository;
import com.capysoft.commerce.ticket.domain.repository.SeatTicketRepository;
import com.capysoft.commerce.ticket.domain.repository.TicketLogRepository;
import com.capysoft.commerce.ticket.domain.repository.TicketRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pago fallido u orden cancelada: las sillas de todas sus entradas vuelven a Catálogo. */
@ExtendWith(MockitoExtension.class)
@DisplayName("Liberar las sillas de una orden")
class ReleaseOrderSeatsTest {

    private static final long ORDER_ID = 40L;
    private static final long BUYER_ID = 7L;

    @Mock OrderRepository orderRepository;
    @Mock TicketRepository ticketRepository;
    @Mock SeatTicketRepository seatTicketRepository;
    @Mock TicketLogRepository ticketLogRepository;
    @Mock CatalogSeatsPort catalogSeatsPort;

    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.builder().orderId(ORDER_ID).userId(BUYER_ID).eventId(5L)
                .totalAmount(new Money(BigDecimal.TEN, "COP")).status(OrderStatus.PAYMENT_PENDING).build();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(ticketRepository.findByOrderId(ORDER_ID)).thenReturn(List.of(
                Ticket.builder().ticketId(100L).orderId(ORDER_ID).status(TicketStatus.PROCESSING).build(),
                Ticket.builder().ticketId(101L).orderId(ORDER_ID).status(TicketStatus.PROCESSING).build()));
        when(seatTicketRepository.findByTicketId(100L)).thenReturn(List.of(SeatTicket.builder().seatId(10).ticketId(100L).build()));
        when(seatTicketRepository.findByTicketId(101L)).thenReturn(List.of(SeatTicket.builder().seatId(11).ticketId(101L).build()));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("pago fallido → cancela la orden y libera sus sillas con el motivo, sin usuario")
    void failedPayment_releasesSeats() {
        new FailOrderPaymentUseCaseImpl(orderRepository, ticketRepository, seatTicketRepository,
                ticketLogRepository, catalogSeatsPort).failOrderPayment(ORDER_ID, "DECLINED");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(catalogSeatsPort).release(ORDER_ID, List.of(10, 11), null, "Payment failed: DECLINED");
    }

    @Test
    @DisplayName("cancelación del comprador → libera sus sillas a su nombre")
    void cancelledByBuyer_releasesSeatsAsBuyer() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("7", null));

        newCancelOrder().execute(ORDER_ID, BUYER_ID);

        verify(catalogSeatsPort).release(ORDER_ID, List.of(10, 11), 7, "Order cancelled");
    }

    @Test
    @DisplayName("cancelación sin un userId en la sesión → libera las sillas sin usuario")
    void cancelledWithoutUserId_releasesSeatsWithoutUser() {
        newCancelOrder().execute(ORDER_ID, BUYER_ID);

        verify(catalogSeatsPort).release(org.mockito.ArgumentMatchers.eq(ORDER_ID),
                org.mockito.ArgumentMatchers.eq(List.of(10, 11)), isNull(), org.mockito.ArgumentMatchers.eq("Order cancelled"));
    }

    private CancelOrderUseCaseImpl newCancelOrder() {
        return new CancelOrderUseCaseImpl(orderRepository, ticketRepository, seatTicketRepository,
                ticketLogRepository, catalogSeatsPort);
    }
}
