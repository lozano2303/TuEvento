package com.capysoft.commerce.payment.application.usecase;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.commerce.payment.application.dto.response.AdminRefundResponse;
import com.capysoft.commerce.payment.application.port.in.GetCommerceAdminStatsPort;
import com.capysoft.commerce.payment.domain.model.PaymentStatus;
import com.capysoft.commerce.payment.domain.model.RefundStatus;
import com.capysoft.commerce.payment.infrastructure.persistence.repository.PaymentJpaRepository;
import com.capysoft.commerce.payment.application.port.out.BuyerDirectoryPort;
import com.capysoft.commerce.payment.infrastructure.persistence.repository.AdminRefundRow;
import com.capysoft.commerce.payment.infrastructure.persistence.repository.RefundJpaRepository;
import com.capysoft.commerce.ticket.application.port.out.EventLookupPort;
import com.capysoft.commerce.ticket.application.port.out.EventLookupPort.EventRef;
import com.capysoft.commerce.ticket.infrastructure.persistence.repository.TicketJpaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** ticket, payment y wallet son el mismo servicio (Comercio): aquí sí se leen sus repositorios. */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetCommerceAdminStatsUseCase implements GetCommerceAdminStatsPort {

    private final TicketJpaRepository  ticketJpaRepository;
    private final PaymentJpaRepository paymentJpaRepository;
    private final RefundJpaRepository  refundJpaRepository;
    private final BuyerDirectoryPort   buyerDirectoryPort;
    private final EventLookupPort      eventLookupPort;

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
        List<AdminRefundRow> rows = refundJpaRepository.findRecentForAdmin(PageRequest.of(0, limit));
        Map<Integer, BuyerDirectoryPort.Buyer> buyers = findBuyers(rows);
        Map<Long, EventRef> events = eventLookupPort.findAll(rows.stream().map(AdminRefundRow::getEventId).toList());
        return rows.stream().map(r -> {
            BuyerDirectoryPort.Buyer buyer = r.getUserId() == null ? null : buyers.get(r.getUserId().intValue());
            EventRef event = events.get(r.getEventId());
            return new AdminRefundResponse(r.getRefundId(), r.getPaymentId(), r.getOrderId(), r.getUserId(),
                    buyer == null ? null : buyer.alias(), buyer == null ? null : buyer.fullName(),
                    buyer == null ? null : buyer.email(), r.getEventId(), event == null ? null : event.name(), r.getGatewayAmount(),
                    r.getWalletAmount(), r.getCurrency(), r.getStatus(), r.getReason(), r.getRequestedAt(),
                    r.getProcessedAt());
        }).toList();
    }

    /** Todos los compradores de la página en una sola consulta; sin Identidad, la lista sale sin ellos. */
    private Map<Integer, BuyerDirectoryPort.Buyer> findBuyers(List<AdminRefundRow> rows) {
        List<Integer> userIds = rows.stream().map(AdminRefundRow::getUserId).filter(Objects::nonNull)
                .map(Long::intValue).distinct().toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        try {
            return buyerDirectoryPort.findBuyers(userIds);
        } catch (Exception e) {
            log.warn("Admin refunds: buyers not resolved: {}", e.getMessage());
            return Map.of();
        }
    }
}
