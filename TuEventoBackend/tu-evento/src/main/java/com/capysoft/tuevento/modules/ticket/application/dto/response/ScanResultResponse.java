package com.capysoft.tuevento.modules.ticket.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Resultado de escanear un QR (ticket individual u orden completa).
 *
 * Para un QR de orden, {@code validatedTickets} lista los tickets que pasaron a
 * USED en este escaneo y {@code alreadyUsedTickets} los que ya estaban usados
 * (p. ej. escaneados antes de forma individual). Un QR de orden con sillas ya
 * escaneadas simplemente valida las restantes sin fallar.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanResultResponse {

    /** Alcance del QR escaneado: "TICKET" o "ORDER". */
    private String scope;
    private Long eventId;
    private Long orderId;

    /** Tickets marcados como USED en este escaneo. */
    private List<ScannedTicket> validatedTickets;
    /** Tickets de la orden que ya estaban USED antes de este escaneo. */
    private List<ScannedTicket> alreadyUsedTickets;

    private int validatedCount;
    private int alreadyUsedCount;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScannedTicket {
        private Long ticketId;
        private String code;
    }
}
