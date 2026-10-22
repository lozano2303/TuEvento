package com.capysoft.tuevento.modules.ticket.domain.port;

import com.capysoft.tuevento.modules.ticket.domain.port.QrTokenPort.QrScope;

/**
 * Puerto de salida para el almacén de nonces de QR de un solo uso.
 *
 * Por cada ticket u orden se guarda el nonce del ÚLTIMO QR generado, con un TTL
 * igual a la vida del token. Reglas:
 *   - store(...) sobreescribe el nonce anterior → al regenerar, el QR previo
 *     deja de ser válido (su nonce ya no coincide). Esto evita la "reventa".
 *   - isCurrent(...) comprueba que un nonce entrante sea el vigente.
 *   - consume(...) valida y elimina el nonce de forma atómica (un solo uso).
 *
 * El almacenamiento es efímero: la caducidad real la garantiza el exp del JWT,
 * este store solo refuerza el anti-reventa y el consumo único.
 */
public interface QrNonceStore {

    /** Guarda (o reemplaza) el nonce vigente para el objetivo, con TTL en segundos. */
    void store(QrScope scope, Long targetId, String nonce, long ttlSeconds);

    /**
     * Valida y consume el nonce de forma atómica.
     *
     * @return true si el nonce era el vigente (y quedó consumido); false si no
     *         coincidía, ya fue usado, o expiró.
     */
    boolean consume(QrScope scope, Long targetId, String nonce);

    /** Elimina el nonce vigente del objetivo (p. ej. al cancelar/expirar manualmente). */
    void invalidate(QrScope scope, Long targetId);
}
