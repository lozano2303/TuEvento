package com.capysoft.tuevento.shared.domain.outbox;

/**
 * Estado de publicación de un {@link OutboxEvent}.
 *
 * <ul>
 *   <li>{@code PENDING}: falta publicarlo; puede estar esperando su backoff.
 *   <li>{@code PUBLISHED}: Kafka confirmó el envío.
 *   <li>{@code FAILED}: agotó los reintentos. No se publicó; se conserva para investigarlo y
 *       recuperarlo con {@code OutboxRecoveryService}. Bloquea los eventos posteriores de su agregado.
 * </ul>
 */
public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
