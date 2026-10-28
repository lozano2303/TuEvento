package com.capysoft.tuevento.modules.identitybridge.port;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lo que el panel de administración de Identidad necesita de Catálogo y de Comercio. Identidad
 * compone el resumen (estadísticas y actividad) sin leer sus tablas; hoy los adaptadores delegan
 * en el monolito y al separar los servicios serán clientes REST.
 */
public interface AdminPanelSourcesPort {

    CatalogStats catalogStats();

    long eventsPendingReview();

    List<RecentEvent> recentlyUpdatedEvents(int limit);

    CommerceStats commerceStats();

    List<RecentPayment> recentPayments(int limit);

    record CatalogStats(long total, long published, long pendingReview, long rejected,
                        long cancelled, long completed, long draft) {
    }

    record RecentEvent(String eventName, String status, Long organizerUserId, LocalDateTime occurredAt) {
    }

    record CommerceStats(long totalTickets, BigDecimal totalRevenue, long approvedPayments,
                         long pendingRefunds, BigDecimal totalRefundedAmount, long processedRefunds) {
    }

    record RecentPayment(boolean approved, BigDecimal amount, String currency, Long orderId,
                         LocalDateTime processedAt) {
    }
}
