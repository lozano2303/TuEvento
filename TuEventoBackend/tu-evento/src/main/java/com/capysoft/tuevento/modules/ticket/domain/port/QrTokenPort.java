package com.capysoft.tuevento.modules.ticket.domain.port;

import java.util.Optional;

/**
 * Puerto de salida para firmar y verificar los tokens QR de tickets.
 *
 * El token es un JWT firmado y de vida corta (ver app.qr.expiration). Codifica
 * a qué apunta el QR (un ticket individual o una orden completa), el comprador
 * dueño, y un nonce de un solo uso que permite invalidar QRs anteriores al
 * regenerar (anti-reventa).
 */
public interface QrTokenPort {

    /** Tipo de alcance del QR: un ticket individual o una orden completa. */
    enum QrScope { TICKET, ORDER }

    /**
     * Datos decodificados de un token QR válido.
     *
     * @param scope    si el QR es de un ticket o de una orden
     * @param targetId ticketId (scope TICKET) u orderId (scope ORDER)
     * @param eventId  evento al que pertenece el QR
     * @param userId   comprador dueño del QR
     * @param nonce    identificador único de esta generación (para single-use)
     */
    record QrPayload(QrScope scope, Long targetId, Long eventId, Long userId, String nonce) {}

    /**
     * Genera un token QR firmado con expiración corta.
     *
     * @return el JWT compacto listo para renderizar como QR en el cliente
     */
    String generate(QrScope scope, Long targetId, Long eventId, Long userId, String nonce);

    /**
     * Verifica firma y expiración del token y devuelve su payload.
     *
     * @return el payload si el token es válido y no ha expirado; vacío en caso contrario
     */
    Optional<QrPayload> parse(String token);

    /** Segundos de vida del token QR (para informar al cliente). */
    long getExpirationSeconds();
}
