package com.capysoft.tuevento.modules.identitybridge.adapter;

import java.util.List;

import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.event.application.port.in.GetEventAdminStatsPort;
import com.capysoft.tuevento.modules.payment.application.port.in.GetCommerceAdminStatsPort;
import com.capysoft.tuevento.modules.identitybridge.port.AdminPanelSourcesPort;

import lombok.RequiredArgsConstructor;

/**
 * Único punto de Identidad que conoce a Catálogo y Comercio para el panel de administración.
 * Hoy delega en sus casos de uso del monolito; al separarlos se sustituye por clientes REST.
 */
@Component
@RequiredArgsConstructor
class AdminPanelSourcesAdapter implements AdminPanelSourcesPort {

    private final GetEventAdminStatsPort    eventStats;
    private final GetCommerceAdminStatsPort commerceStats;

    @Override
    public CatalogStats catalogStats() {
        var s = eventStats.getStats();
        return new CatalogStats(s.total(), s.published(), s.pendingReview(), s.rejected(),
                s.cancelled(), s.completed(), s.draft());
    }

    @Override
    public long eventsPendingReview() {
        return eventStats.countPendingReview();
    }

    @Override
    public List<RecentEvent> recentlyUpdatedEvents(int limit) {
        return eventStats.findRecentlyUpdated(limit).stream()
                .map(e -> new RecentEvent(e.eventName(), e.status(), e.organizerUserId(), e.occurredAt()))
                .toList();
    }

    @Override
    public CommerceStats commerceStats() {
        var s = commerceStats.getStats();
        return new CommerceStats(s.totalTickets(), s.totalRevenue(), s.approvedPayments(),
                s.pendingRefunds(), s.totalRefundedAmount(), s.processedRefunds());
    }

    @Override
    public List<RecentPayment> recentPayments(int limit) {
        return commerceStats.findRecentPayments(limit).stream()
                .map(p -> new RecentPayment(p.approved(), p.amount(), p.currency(), p.orderId(), p.processedAt()))
                .toList();
    }
}
