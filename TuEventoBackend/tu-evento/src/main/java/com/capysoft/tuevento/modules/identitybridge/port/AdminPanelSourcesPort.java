package com.capysoft.tuevento.modules.identitybridge.port;

import java.time.LocalDateTime;
import java.util.List;

/**
 * What Identity's admin panel needs from Catalog. Identity builds the summary (figures and
 * activity) without reading Catalog's tables; Commerce's figures it asks commerce-service directly.
 */
public interface AdminPanelSourcesPort {

    CatalogStats catalogStats();

    long eventsPendingReview();

    List<RecentEvent> recentlyUpdatedEvents(int limit);

    record CatalogStats(long total, long published, long pendingReview, long rejected,
                        long cancelled, long completed, long draft) {
    }

    record RecentEvent(String eventName, String status, Long organizerUserId, LocalDateTime occurredAt) {
    }
}
