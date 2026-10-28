package com.capysoft.tuevento.shared.infrastructure.websocket;

/**
 * Los contadores de pendientes del panel de admin cambiaron fuera del monolito (una solicitud de
 * organizador creada, aprobada o rechazada en identity-service): quien los publica debe
 * recalcularlos y enviarlos a {@code /topic/admin/pending-counts}.
 */
public record AdminPendingCountsChangedEvent() {
}
