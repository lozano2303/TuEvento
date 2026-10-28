package com.capysoft.tuevento.modules.payment.application.usecase;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.tuevento.modules.payment.application.dto.response.AdminRefundResponse;
import com.capysoft.tuevento.modules.payment.application.port.in.GetCommerceAdminStatsPort;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentStatus;
import com.capysoft.tuevento.modules.payment.domain.model.RefundStatus;
import com.capysoft.tuevento.modules.payment.infrastructure.persistence.repository.PaymentJpaRepository;
import com.capysoft.tuevento.modules.payment.infrastructure.persistence.repository.RefundJpaRepository;
import com.capysoft.tuevento.modules.ticket.infrastructure.persistence.repository.TicketJpaRepository;

import lombok.RequiredArgsConstructor;

/** ticket, payment y wallet son el mismo servicio (Comercio): aquí sí se leen sus repositorios. */
@Service
@RequiredArgsConstructor
public class GetCommerceAdminStatsUseCase implements GetCommerceAdminStatsPort {

    private final TicketJpaRepository  ticketJpaRepository;
    private final PaymentJpaRepository paymentJpaRepository;
    private final RefundJpaRepository  refundJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public CommerceStats getStats() {
        return new CommerceStats(
                ticketJpaRepository.count(),
                paymentJpaRepository.sumApprovedRevenue(),
                paymentJpaRepository.countByStatus(PaymentStatus.APPROVED),
                refundJpaRepository.countByStatus(RefundStatus.REQUESTED),
                paymentJpaRepository.sumTotalRefundedAmount(),
                refundJpaRepository.countByStatus(RefundStatus.PROCESSED));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecentPayment> findRecentPayments(int limit) {
        if (limit < 1) {
            return List.of();
        }
        return paymentJpaRepository.findTopByOrderByProcessedAtDesc(PageRequest.of(0, limit)).stream()
                .map(p -> new RecentPayment(p.getStatus() == PaymentStatus.APPROVED, p.getAmount(),
                        p.getCurrency(), p.getOrderId(), p.getProcessedAt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminRefundResponse> findRecentRefunds(int limit) {
        return refundJpaRepository.findRecentForAdmin(PageRequest.of(0, limit));
    }
}
