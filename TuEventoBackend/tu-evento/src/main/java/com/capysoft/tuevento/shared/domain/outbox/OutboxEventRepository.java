package com.capysoft.tuevento.shared.domain.outbox;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Puerto de dominio para el repositorio del outbox.
 *
 * <p>El método {@link #save} debe llamarse dentro de la misma transacción que
 * el cambio de negocio para garantizar atomicidad.
 */
public interface OutboxEventRepository {

    /**
     * Persiste un evento en el outbox como {@link OutboxStatus#PENDING}. Llamar dentro de la
     * transacción de negocio. Si el evento no trae {@code eventId}, se le asigna uno nuevo.
     */
    OutboxEvent save(OutboxEvent event);

    /**
     * Devuelve hasta {@code limit} eventos listos para publicar, ordenados por
     * {@code created_at ASC} (FIFO): PENDING, con su backoff cumplido y sin ningún evento
     * anterior de su mismo agregado en PENDING o FAILED. Así un agregado nunca publica fuera de
     * orden y los demás agregados siguen avanzando.
     */
    List<OutboxEvent> findPendingBatch(int limit, LocalDateTime now);

    /** Marca el evento como publicado. */
    void markPublished(Long id, LocalDateTime now);

    /**
     * Suma un reintento, guarda el error y programa el siguiente intento para
     * {@code nextAttemptAt}. El evento sigue PENDING.
     */
    void scheduleRetry(Long id, String errorMessage, LocalDateTime now, LocalDateTime nextAttemptAt);

    /**
     * Suma el último reintento y deja el evento en {@link OutboxStatus#FAILED}, sin marcarlo
     * como publicado. Conserva payload, topic y error.
     */
    void markExhausted(Long id, String errorMessage, LocalDateTime now);

    /**
     * Devuelve un evento FAILED a PENDING con el contador a cero. Conserva su {@code eventId}
     * y su último error. Devuelve {@code false} si no existe o no estaba FAILED.
     */
    boolean requeueFailed(UUID eventId, LocalDateTime now);
}
