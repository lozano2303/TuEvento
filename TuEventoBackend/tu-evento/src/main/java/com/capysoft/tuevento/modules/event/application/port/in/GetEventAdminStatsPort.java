package com.capysoft.tuevento.modules.event.application.port.in;

import java.time.LocalDateTime;
import java.util.List;

/** Cifras de Catálogo para el panel de administración, sin exponer sus tablas. */
public interface GetEventAdminStatsPort {

    EventStats getStats();

    long countPendingReview();

    /** Los eventos modificados más recientemente, del más nuevo al más viejo. */
    List<RecentEvent> findRecentlyUpdated(int limit);

    record EventStats(long total, long published, long pendingReview, long rejected,
                      long cancelled, long completed, long draft) {
    }

    /**
     * @param status     nombre del estado ({@code PUBLISHED}, {@code REJECTED}…)
     * @param occurredAt última modificación, o la creación si nunca se modificó
     */
    record RecentEvent(String eventName, String status, Long organizerUserId, LocalDateTime occurredAt) {
    }
}
