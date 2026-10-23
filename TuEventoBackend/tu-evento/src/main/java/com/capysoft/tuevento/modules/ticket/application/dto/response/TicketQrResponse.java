package com.capysoft.tuevento.modules.ticket.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response con el token QR firmado que el cliente renderiza como código QR.
 * Vive pocos segundos (expiresInSeconds); regenerarlo invalida el anterior.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketQrResponse {
    /** Alcance del QR: "TICKET" o "ORDER". */
    private String scope;
    /** ticketId (scope TICKET) u orderId (scope ORDER). */
    private Long targetId;
    /** JWT firmado a codificar en el QR. */
    private String qrToken;
    /** Momento exacto de expiración del token. */
    private LocalDateTime expiresAt;
    /** Segundos de vida del token (para la cuenta regresiva en el cliente). */
    private long expiresInSeconds;
}
