package com.capysoft.tuevento.shared.infrastructure.outbox;

import com.capysoft.tuevento.shared.domain.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Recupera eventos del outbox que agotaron los reintentos.
 *
 * <p>Un evento FAILED vuelve a PENDING con el contador a cero y el mismo {@code eventId}, así
 * que si alguna vez llegó a Kafka el consumidor lo reconoce como duplicado. Mientras sigue
 * FAILED bloquea los eventos posteriores de su agregado; al recuperarlo, salen en orden.
 *
 * <p>Sin endpoint por ahora. Para localizar los candidatos:
 * <pre>
 * SELECT event_id, aggregate_type, aggregate_id, event_type, retry_count, last_error, failed_at
 * FROM outbox_event WHERE status = 'FAILED' ORDER BY created_at;
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxRecoveryService {

    private final OutboxEventRepository outboxEventRepository;
    private final Clock clock;

    /**
     * Devuelve el evento a la cola de publicación.
     *
     * @return {@code false} si no existe o no estaba FAILED (nada cambia)
     */
    @Transactional
    public boolean requeue(UUID eventId) {
        boolean requeued = outboxEventRepository.requeueFailed(eventId, now());
        if (requeued) {
            log.info("[OutboxRecovery] Event eventId={} requeued from FAILED to PENDING", eventId);
        } else {
            log.warn("[OutboxRecovery] Event eventId={} not requeued: not found or not FAILED", eventId);
        }
        return requeued;
    }

    /**
     * Instante del {@link Clock} (fijo en los tests) en la zona de la JVM, la misma con la que se
     * guardan {@code created_at} y {@code processed_at}: así todas las fechas del outbox son comparables.
     */
    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneId.systemDefault());
    }
}
