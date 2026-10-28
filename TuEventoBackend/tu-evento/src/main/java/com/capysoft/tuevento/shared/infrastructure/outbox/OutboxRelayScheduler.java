package com.capysoft.tuevento.shared.infrastructure.outbox;

import com.capysoft.tuevento.shared.domain.outbox.OutboxEvent;
import com.capysoft.tuevento.shared.domain.outbox.OutboxEventRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Relay del patrón Transactional Outbox.
 *
 * <p>Cada {@code outbox.relay.interval-ms} milisegundos (defecto: 1 000 ms) lee un
 * lote de eventos listos, los publica a Kafka y los marca como publicados. Si la
 * publicación falla, suma un reintento, guarda el error y programa el siguiente intento
 * con backoff exponencial. Al agotar los reintentos el evento queda FAILED: no se publica,
 * no se marca como procesado y se recupera con {@link OutboxRecoveryService}.
 *
 * <h3>Garantías</h3>
 * <ul>
 *   <li>At-least-once: si el proceso muere después del commit a Kafka pero antes de
 *       marcarlo publicado, el evento se publicará de nuevo con el mismo header
 *       {@link OutboxHeaders#EVENT_ID}. Los consumidores deben ser idempotentes.
 *   <li>Orden por agregado: un evento no sale mientras otro anterior de su mismo agregado
 *       siga PENDING o FAILED (ver {@link OutboxEventJpaRepository#findReadyBatch}). Los
 *       demás agregados siguen avanzando.
 *   <li>Ante un fallo el lote se corta: con Kafka caído no se gastan los reintentos de los
 *       demás eventos ni se retienen sus locks durante un timeout por cada uno.
 *   <li>Backpressure: el batch está limitado a {@code outbox.relay.batch-size}
 *       (defecto: 100) para no saturar Kafka.
 * </ul>
 *
 * <h3>Configuración</h3>
 * <pre>
 * outbox.relay.enabled=true
 * outbox.relay.batch-size=100
 * outbox.relay.interval-ms=1000
 * outbox.relay.max-retries=10              # al llegar a este número de fallos el evento queda FAILED
 * outbox.relay.backoff-initial-ms=1000     # espera tras el primer fallo; se duplica en cada uno
 * outbox.relay.backoff-max-ms=300000       # tope de la espera entre intentos
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Clock clock;

    @Value("${outbox.relay.enabled:true}")
    private boolean enabled;

    @Value("${outbox.relay.batch-size:100}")
    private int batchSize;

    @Value("${outbox.relay.max-retries:10}")
    private int maxRetries;

    @Value("${outbox.relay.backoff-initial-ms:1000}")
    private long backoffInitialMs;

    @Value("${outbox.relay.backoff-max-ms:300000}")
    private long backoffMaxMs;

    @Value("${outbox.relay.send-timeout-ms:10000}")
    private long sendTimeoutMs;

    /** Falla al arrancar con una configuración que dejaría eventos sin reintento o con esperas inválidas. */
    @PostConstruct
    void validateConfig() {
        if (maxRetries < 1) {
            throw new IllegalStateException("outbox.relay.max-retries must be >= 1, was " + maxRetries);
        }
        if (backoffInitialMs < 1 || backoffMaxMs < backoffInitialMs) {
            throw new IllegalStateException("outbox.relay.backoff-initial-ms must be >= 1 and <= backoff-max-ms, was "
                    + backoffInitialMs + " / " + backoffMaxMs);
        }
        if (batchSize < 1 || sendTimeoutMs < 1) {
            throw new IllegalStateException("outbox.relay.batch-size and send-timeout-ms must be >= 1");
        }
    }

    @Scheduled(fixedDelayString = "${outbox.relay.interval-ms:1000}")
    @Transactional
    public void relay() {
        if (!enabled) {
            return;
        }

        List<OutboxEvent> pending = outboxEventRepository.findPendingBatch(batchSize, now());

        if (pending.isEmpty()) {
            return;
        }

        log.debug("[OutboxRelay] Processing {} event(s).", pending.size());

        for (OutboxEvent event : pending) {
            if (!processOne(event)) {
                break;
            }
        }
    }

    /** Publica un evento; devuelve {@code false} si falló y el lote debe cortarse. */
    private boolean processOne(OutboxEvent event) {
        try {
            // La key del mensaje es aggregateId para garantizar orden por agregado
            // dentro del mismo topic (Kafka asigna la misma partición por key).
            // Envío síncrono: el resultado se marca dentro de la transacción que tiene el lock
            // de la fila; con un callback asíncrono la marca llegaría tras el commit, sin lock.
            kafkaTemplate.send(toRecord(event)).get(sendTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            registerFailure(event, "interrupted");
            return false;
        } catch (Exception e) {
            log.error("[OutboxRelay] Kafka send failed for event {} (eventId={}): {}",
                    event.getId(), event.getEventId(), e.getMessage());
            registerFailure(event, e.getMessage());
            return false;
        }
        // Fuera del try: si falla la marca, la transacción se deshace y el evento se reenvía
        // con el mismo eventId en la siguiente ejecución, en vez de contarse como fallo de Kafka.
        outboxEventRepository.markPublished(event.getId(), now());
        log.debug("[OutboxRelay] Published event {} (eventId={}) to topic {}",
                event.getId(), event.getEventId(), event.getTopic());
        return true;
    }

    private void registerFailure(OutboxEvent event, String error) {
        LocalDateTime now = now();
        int failures = event.getRetryCount() + 1;
        if (failures >= maxRetries) {
            outboxEventRepository.markExhausted(event.getId(), error, now);
            log.error("[OutboxRelay] Event {} exhausted {} retries and is now FAILED. eventId={}, "
                            + "aggregateType={}, aggregateId={}, eventType={}, topic={}, lastError={}",
                    event.getId(), failures, event.getEventId(), event.getAggregateType(),
                    event.getAggregateId(), event.getEventType(), event.getTopic(), error);
            return;
        }
        outboxEventRepository.scheduleRetry(event.getId(), error, now, now.plus(backoff(failures)));
    }

    /** {@code backoff-initial-ms * 2^(fallos-1)}, sin pasar de {@code backoff-max-ms}. */
    Duration backoff(int failures) {
        int exponent = Math.min(Math.max(failures - 1, 0), 30);
        long delay = backoffInitialMs * (1L << exponent);
        if (delay <= 0 || delay > backoffMaxMs) {
            delay = backoffMaxMs;
        }
        return Duration.ofMillis(delay);
    }

    private static ProducerRecord<String, String> toRecord(OutboxEvent event) {
        RecordHeaders headers = new RecordHeaders();
        addHeader(headers, OutboxHeaders.EVENT_ID, String.valueOf(event.getEventId()));
        addHeader(headers, OutboxHeaders.EVENT_TYPE, event.getEventType());
        addHeader(headers, OutboxHeaders.AGGREGATE_TYPE, event.getAggregateType());
        addHeader(headers, OutboxHeaders.AGGREGATE_ID, event.getAggregateId());
        addHeader(headers, OutboxHeaders.SCHEMA_VERSION, OutboxHeaders.CURRENT_SCHEMA_VERSION);
        if (event.getCreatedAt() != null) {
            // created_at está en la zona de la JVM; el header viaja como instante UTC (2026-10-27T15:00:00Z)
            addHeader(headers, OutboxHeaders.OCCURRED_AT,
                    event.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toString());
        }
        return new ProducerRecord<>(event.getTopic(), null, event.getAggregateId(), event.getPayload(), headers);
    }

    private static void addHeader(RecordHeaders headers, String name, String value) {
        if (value != null) {
            headers.add(name, value.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Instante del {@link Clock} (fijo en los tests) en la zona de la JVM, la misma con la que se
     * guardan {@code created_at} y {@code processed_at}: así todas las fechas del outbox son comparables.
     */
    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneId.systemDefault());
    }
}
