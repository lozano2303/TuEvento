package com.capysoft.tuevento.modules.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.capysoft.tuevento.modules.payment.application.dto.response.AdminRefundResponse;
import com.capysoft.tuevento.modules.payment.application.port.in.GetCommerceAdminStatsPort.CommerceStats;
import com.capysoft.tuevento.modules.payment.application.port.in.GetCommerceAdminStatsPort.RecentPayment;
import com.capysoft.tuevento.modules.payment.application.port.out.BuyerDirectoryPort;
import com.capysoft.tuevento.modules.payment.application.port.out.BuyerDirectoryPort.Buyer;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentStatus;
import com.capysoft.tuevento.modules.payment.domain.model.RefundStatus;
import com.capysoft.tuevento.modules.payment.infrastructure.persistence.entity.PaymentEntity;
import com.capysoft.tuevento.modules.payment.infrastructure.persistence.repository.AdminRefundRow;
import com.capysoft.tuevento.modules.payment.infrastructure.persistence.repository.PaymentJpaRepository;
import com.capysoft.tuevento.modules.payment.infrastructure.persistence.repository.RefundJpaRepository;
import com.capysoft.tuevento.modules.ticket.infrastructure.persistence.repository.TicketJpaRepository;

@DisplayName("GetCommerceAdminStatsUseCase")
class GetCommerceAdminStatsUseCaseTest {

    private final TicketJpaRepository tickets = mock(TicketJpaRepository.class);
    private final PaymentJpaRepository payments = mock(PaymentJpaRepository.class);
    private final RefundJpaRepository refunds = mock(RefundJpaRepository.class);
    private final BuyerDirectoryPort buyers = mock(BuyerDirectoryPort.class);
    private final GetCommerceAdminStatsUseCase useCase = new GetCommerceAdminStatsUseCase(tickets, payments, refunds, buyers);

    private static AdminRefundRow refund(Long userId) {
        AdminRefundRow row = mock(AdminRefundRow.class);
        when(row.getRefundId()).thenReturn(1L);
        when(row.getUserId()).thenReturn(userId);
        when(row.getStatus()).thenReturn(RefundStatus.REQUESTED);
        return row;
    }

    @Test
    @DisplayName("adds up tickets, revenue, payments and refunds")
    void stats() {
        when(tickets.count()).thenReturn(30L);
        when(payments.sumApprovedRevenue()).thenReturn(new BigDecimal("150.00"));
        when(payments.countByStatus(PaymentStatus.APPROVED)).thenReturn(12L);
        when(refunds.countByStatus(RefundStatus.REQUESTED)).thenReturn(2L);
        when(payments.sumTotalRefundedAmount()).thenReturn(new BigDecimal("20.00"));
        when(refunds.countByStatus(RefundStatus.PROCESSED)).thenReturn(1L);

        assertThat(useCase.getStats())
                .isEqualTo(new CommerceStats(30, new BigDecimal("150.00"), 12, 2, new BigDecimal("20.00"), 1));
    }

    @Test
    @DisplayName("lists the latest payments, and nothing for a limit below one")
    void recentPayments() {
        LocalDateTime at = LocalDateTime.of(2026, 10, 27, 12, 0);
        when(payments.findTopByOrderByProcessedAtDesc(any())).thenReturn(List.of(
                PaymentEntity.builder().status(PaymentStatus.APPROVED).amount(BigDecimal.TEN).currency("COP")
                        .orderId(5L).processedAt(at).build(),
                PaymentEntity.builder().status(PaymentStatus.REJECTED).amount(BigDecimal.ONE).currency("COP")
                        .orderId(6L).processedAt(at).build()));

        assertThat(useCase.findRecentPayments(2)).containsExactly(
                new RecentPayment(true, BigDecimal.TEN, "COP", 5L, at),
                new RecentPayment(false, BigDecimal.ONE, "COP", 6L, at));
        assertThat(useCase.findRecentPayments(0)).isEmpty();
    }

    @Test
    @DisplayName("fills each refund with its buyer from identity in a single lookup")
    void refundsWithBuyers() {
        List<AdminRefundRow> rows = List.of(refund(7L), refund(null));
        when(refunds.findRecentForAdmin(any())).thenReturn(rows);
        when(buyers.findBuyers(List.of(7))).thenReturn(Map.of(7, new Buyer("ana", "Ana Perez", "ana@example.com")));

        List<AdminRefundResponse> result = useCase.findRecentRefunds(10);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).alias()).isEqualTo("ana");
        assertThat(result.get(0).email()).isEqualTo("ana@example.com");
        assertThat(result.get(1).alias()).isNull();
    }

    @Test
    @DisplayName("lists refunds without buyers when identity is unavailable or no refund has a user")
    void refundsWithoutBuyers() {
        List<AdminRefundRow> withUser = List.of(refund(7L));
        when(refunds.findRecentForAdmin(any())).thenReturn(withUser);
        when(buyers.findBuyers(List.of(7))).thenThrow(new IllegalStateException("identity down"));

        assertThat(useCase.findRecentRefunds(10)).singleElement()
                .satisfies(refund -> assertThat(refund.fullName()).isNull());

        BuyerDirectoryPort unused = mock(BuyerDirectoryPort.class);
        List<AdminRefundRow> withoutUser = List.of(refund(null));
        when(refunds.findRecentForAdmin(any())).thenReturn(withoutUser);
        assertThat(new GetCommerceAdminStatsUseCase(tickets, payments, refunds, unused).findRecentRefunds(10)).hasSize(1);
        verifyNoInteractions(unused);
    }
}
