package com.capysoft.identity.security.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AdminDashboardStatsResponse {

    // ── Usuarios ──────────────────────────────────────────────────────────────
    private final long totalUsers;

    // ── Eventos ───────────────────────────────────────────────────────────────
    private final long totalEvents;
    private final long publishedEvents;
    private final long pendingReviewEvents;
    private final long rejectedEvents;
    private final long cancelledEvents;
    private final long completedEvents;
    private final long draftEvents;

    // ── Tickets ───────────────────────────────────────────────────────────────
    private final long totalTickets;

    // ── Solicitudes de organizador ────────────────────────────────────────────
    private final long pendingOrganizerRequests;
    private final long approvedOrganizerRequests;

    // ── Pagos ─────────────────────────────────────────────────────────────────
    private final BigDecimal totalRevenue;          // SUM(amount) WHERE status='APPROVED'
    private final long       approvedPayments;
    private final long       pendingRefunds;
    private final BigDecimal totalRefundedAmount;   // SUM(amount + walletAmountApplied) WHERE status='REFUNDED'
    private final long       processedRefunds;       // COUNT WHERE refund.status='PROCESSED'
}
