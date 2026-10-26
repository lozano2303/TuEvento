package com.capysoft.tuevento.shared.infrastructure.outbox;

import com.capysoft.tuevento.shared.domain.outbox.OutboxEvent;
import com.capysoft.tuevento.shared.domain.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Relay del patrón Transactional Outbox.
 *
 * <p>Cada {@code outbox.relay.interval-ms} milisegundos (defecto: 1 000 ms) lee un
 * lote de eventos no procesados del outbox, los publica a Kafka y los marca como
 * procesados. Si la publicación a Kafka falla, incrementa {@code retry_count} y
 * guarda el error — el evento se reintentará en la siguiente ejecución.
 *
 * <h3>Garantías</h3>
 * <ul>
 *   <li>At-least-once: si el proceso muere después del commit a Kafka pero antes de
 *       marcar {@code processed=true}, el evento se publicará de nuevo. Los
 *       consumidores deben ser idempotentes.
 *   <li>Orden FIFO dentro del mismo topic por {@code created_at ASC}.
 *   <li>Backpressure: el batch está limitado a {@code outbox.relay.batch-size}
 *       (defecto: 100) para no saturar Kafka.
 * </ul>
 *
 * <h3>Configuración</h3>
 * <pre>
 * outbox.relay.enabled=true
 * outbox.relay.batch-size=100
 * outbox.relay.interval-ms=1000
 * outbox.relay.max-retries=5   # eventos con retry_count >= max-retries se descartan con log ERROR
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${outbox.relay.enabled:true}")
    private boolean enabled;

    @Value("${outbox.relay.batch-size:100}")
    private int batchSize;

    @Value("${outbox.relay.max-retries:5}")
    private int maxRetries;

    @Value("${outbox.relay.send-timeout-ms:10000}")
    private long sendTimeoutMs;

    @Scheduled(fixedDelayString = "${outbox.relay.interval-ms:1000}")
    @Transactional
    public void relay() {
        if (!enabled) {
            return;
        }

        List<OutboxEvent> pending = outboxEventRepository.findPendingBatch(batchSize);

        if (pending.isEmpty()) {
            return;
        }

        log.debug("[OutboxRelay] Processing {} event(s).", pending.size());

        for (OutboxEvent event : pending) {
            processOne(event);
        }
    }

    private void processOne(OutboxEvent event) {
        if (event.getRetryCount() >= maxRetries) {
            log.error("[OutboxRelay] Event {} exceeded max retries ({}) — skipping. " +
                      "aggregateType={}, eventType={}, lastError={}",
                    event.getId(), maxRetries,
                    event.getAggregateType(), event.getEventType(), event.getLastError());
            // Marcar como procesado para no bloquear el relay con eventos irrecuperables
            outboxEventRepository.markProcessed(event.getId());
            return;
        }

        try {
            // La key del mensaje es aggregateId para garantizar orden por agregado
            // dentro del mismo topic (Kafka asigna la misma partición por key).
            // Envío síncrono: el resultado se marca dentro de la transacción que tiene el lock
            // de la fila; con un callback asíncrono la marca llegaría tras el commit, sin lock.
            kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload())
                    .get(sendTimeoutMs, TimeUnit.MILLISECONDS);
            outboxEventRepository.markProcessed(event.getId());
            log.debug("[OutboxRelay] Published event {} to topic {}", event.getId(), event.getTopic());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            outboxEventRepository.markFailed(event.getId(), "interrupted");
        } catch (Exception e) {
            log.error("[OutboxRelay] Kafka send failed for event {}: {}", event.getId(), e.getMessage());
            outboxEventRepository.markFailed(event.getId(), e.getMessage());
        }
    }
}
