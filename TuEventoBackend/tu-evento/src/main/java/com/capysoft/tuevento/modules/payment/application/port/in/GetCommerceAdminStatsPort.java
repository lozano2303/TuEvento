package com.capysoft.tuevento.modules.payment.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.capysoft.tuevento.modules.payment.application.dto.response.AdminRefundResponse;

/** Cifras de Comercio (tickets, pagos y reembolsos) para el panel de administración. */
public interface GetCommerceAdminStatsPort {

    CommerceStats getStats();

    /** Los pagos procesados más recientemente, del más nuevo al más viejo. */
    List<RecentPayment> findRecentPayments(int limit);

    /** Los reembolsos más recientes con el desglose del pago, el comprador y el evento. */
    List<AdminRefundResponse> findRecentRefunds(int limit);

    record CommerceStats(long totalTickets, BigDecimal totalRevenue, long approvedPayments,
                         long pendingRefunds, BigDecimal totalRefundedAmount, long processedRefunds) {
    }

    record RecentPayment(boolean approved, BigDecimal amount, String currency, Long orderId,
                         LocalDateTime processedAt) {
    }
}
