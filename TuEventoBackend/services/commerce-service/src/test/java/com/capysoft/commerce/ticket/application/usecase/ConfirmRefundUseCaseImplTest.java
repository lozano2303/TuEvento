package com.capysoft.commerce.ticket.application.usecase;

import com.capysoft.commerce.ticket.application.dto.response.RefundableTicketsSummary;
import com.capysoft.commerce.ticket.domain.model.Money;
import com.capysoft.commerce.ticket.domain.model.Order;
import com.capysoft.commerce.ticket.domain.model.OrderStatus;
import com.capysoft.commerce.ticket.domain.model.Ticket;
import com.capysoft.commerce.ticket.domain.model.TicketStatus;
import com.capysoft.commerce.ticket.domain.repository.OrderRepository;
import com.capysoft.commerce.ticket.domain.repository.TicketLogRepository;
import com.capysoft.commerce.ticket.domain.repository.TicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Solo se reembolsan los tickets PAID: los USED se conservan y la orden solo pasa
 * a REFUNDED si ninguno se usó.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConfirmRefundUseCaseImpl")
class ConfirmRefundUseCaseImplTest {

    private static final Long ORDER_ID = 20L;

    @Mock private OrderRepository orderRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private TicketLogRepository ticketLogRepository;
    @InjectMocks private ConfirmRefundUseCaseImpl useCase;

    private static Order paidOrder() {
        return Order.builder().orderId(ORDER_ID).status(OrderStatus.PAID)
                .totalAmount(new Money(new BigDecimal("150000"), "COP")).build();
    }

    private static Ticket ticket(long id, TicketStatus status, String price) {
        return Ticket.builder().ticketId(id).orderId(ORDER_ID).status(status)
                .totalPrice(new Money(new BigDecimal(price), "COP")).build();
    }

    @Test
    @DisplayName("resumen → solo suma los tickets PAID y no cuenta los cancelados")
    void summary_countsOnlyPaidTickets() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(paidOrder()));
        when(ticketRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(List.of(
                ticket(1, TicketStatus.USED, "50000"),
                ticket(2, TicketStatus.PAID, "40000"),
                ticket(3, TicketStatus.PAID, "60000"),
                ticket(4, TicketStatus.CANCELLED, "70000")));

        RefundableTicketsSummary summary = useCase.getRefundableTickets(ORDER_ID);

        assertThat(summary.amount()).isEqualByComparingTo("100000");
        assertThat(summary.refundableCount()).isEqualTo(2);
        assertThat(summary.totalCount()).isEqualTo(3);
        assertThat(summary.isPartial()).isTrue();
        assertThat(summary.isNothingToRefund()).isFalse();
    }

    @Test
    @DisplayName("con tickets usados → solo los PAID pasan a REFUNDED y la orden sigue PAID")
    void confirm_withUsedTickets_keepsUsedAndOrderPaid() {
        Order order = paidOrder();
        Ticket used = ticket(1, TicketStatus.USED, "50000");
        Ticket unused = ticket(2, TicketStatus.PAID, "50000");
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(ticketRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(List.of(used, unused));

        RefundableTicketsSummary refunded = useCase.confirmRefund(ORDER_ID);

        assertThat(refunded.refundableCount()).isEqualTo(1);
        assertThat(refunded.amount()).isEqualByComparingTo("50000");
        assertThat(used.getStatus()).isEqualTo(TicketStatus.USED);
        assertThat(unused.getStatus()).isEqualTo(TicketStatus.REFUNDED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository, never()).save(any());
        verify(ticketRepository, never()).save(used);
    }

    @Test
    @DisplayName("sin tickets usados → todos REFUNDED y la orden pasa a REFUNDED")
    void confirm_noUsedTickets_refundsOrder() {
        Order order = paidOrder();
        Ticket first = ticket(1, TicketStatus.PAID, "50000");
        Ticket second = ticket(2, TicketStatus.PAID, "50000");
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(ticketRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(List.of(first, second));

        useCase.confirmRefund(ORDER_ID);

        assertThat(first.getStatus()).isEqualTo(TicketStatus.REFUNDED);
        assertThat(second.getStatus()).isEqualTo(TicketStatus.REFUNDED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        verify(orderRepository).save(order);
    }
}
