package com.capysoft.tuevento.modules.security.application.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Contadores de elementos que esperan acción del administrador.
 * Se exponen por REST (carga inicial) y se emiten por WebSocket en
 * {@code /topic/admin/pending-counts} cada vez que cambian.
 */
@Getter
@Builder
public class AdminPendingCountsResponse {

    /** Solicitudes de organizador en estado PENDING. */
    private final long organizerRequests;

    /** Eventos en estado PENDING_REVIEW. */
    private final long eventsInReview;
}
