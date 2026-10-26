package com.capysoft.tuevento.shared.domain.outbox;

import java.util.List;

/**
 * Puerto de dominio para el repositorio del outbox.
 *
 * <p>El método {@link #save} debe llamarse dentro de la misma transacción que
 * el cambio de negocio para garantizar atomicidad.
 */
public interface OutboxEventRepository {

    /**
     * Persiste un evento en el outbox. Llamar dentro de la transacción de negocio.
     */
    OutboxEvent save(OutboxEvent event);

    /**
     * Devuelve hasta {@code limit} eventos no procesados, ordenados por
     * {@code created_at ASC} (FIFO). Usado por el relay para publicar en orden.
     */
    List<OutboxEvent> findPendingBatch(int limit);

    /**
     * Marca el evento como procesado y registra el momento de publicación.
     */
    void markProcessed(Long id);

    /**
     * Incrementa el contador de reintentos y guarda el mensaje de error.
     */
    void markFailed(Long id, String errorMessage);
}
