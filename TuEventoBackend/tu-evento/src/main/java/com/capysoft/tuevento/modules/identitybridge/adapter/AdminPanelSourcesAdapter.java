package com.capysoft.tuevento.modules.identitybridge.adapter;

import java.util.List;

import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.event.application.port.in.GetEventAdminStatsPort;
import com.capysoft.tuevento.modules.identitybridge.port.AdminPanelSourcesPort;

import lombok.RequiredArgsConstructor;

/** The only place of the identity bridge that knows Catalog, for the admin panel. */
@Component
@RequiredArgsConstructor
class AdminPanelSourcesAdapter implements AdminPanelSourcesPort {

    private final GetEventAdminStatsPort eventStats;

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

}
