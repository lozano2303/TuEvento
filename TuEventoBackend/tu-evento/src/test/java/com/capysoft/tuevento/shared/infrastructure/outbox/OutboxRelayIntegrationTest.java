package com.capysoft.tuevento.shared.infrastructure.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import com.capysoft.tuevento.shared.domain.outbox.OutboxEvent;

/**
 * Relay real sobre el outbox de H2, con Kafka simulado y un reloj que se adelanta a mano:
 * reintentos con backoff, agotamiento, recuperación y orden por agregado de principio a fin.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OutboxEventRepositoryImpl.class)
@DisplayName("Outbox relay de extremo a extremo")
class OutboxRelayIntegrationTest {

    @Autowired OutboxEventRepositoryImpl repository;
    @Autowired JdbcTemplate jdbcTemplate;

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-27T15:00:00Z"));
    private final List<ProducerRecord<String, String>> sent = new ArrayList<>();
    private final AtomicBoolean kafkaUp = new AtomicBoolean(true);

    private OutboxRelayScheduler relay;
    private OutboxRecoveryService recovery;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        relay = new OutboxRelayScheduler(repository, kafkaTemplate, clock);
        ReflectionTestUtils.setField(relay, "enabled", true);
        ReflectionTestUtils.setField(relay, "batchSize", 100);
        ReflectionTestUtils.setField(relay, "maxRetries", 3);
        ReflectionTestUtils.setField(relay, "backoffInitialMs", 1000L);
        ReflectionTestUtils.setField(relay, "backoffMaxMs", 60000L);
        ReflectionTestUtils.setField(relay, "sendTimeoutMs", 1000L);
        recovery = new OutboxRecoveryService(repository, clock);

        when(kafkaTemplate.send(any(ProducerRecord.class))).thenAnswer(inv -> {
            if (!kafkaUp.get()) {
                return CompletableFuture.failedFuture(new IllegalStateException("broker down"));
            }
            sent.add(inv.getArgument(0));
            return CompletableFuture.completedFuture((SendResult<String, String>) null);
        });
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneId.systemDefault());
    }

    private OutboxEvent save(String aggregateId, String eventType, int minutesAgo) {
        return repository.save(OutboxEvent.builder().aggregateType("order").aggregateId(aggregateId)
                .eventType(eventType).topic("tuevento.order").payload("{}")
                .createdAt(now().minusMinutes(minutesAgo)).build());
    }

    private Map<String, Object> row(Long id) {
        return jdbcTemplate.queryForMap("SELECT status, processed, retry_count, failed_at FROM outbox_event WHERE id = ?", id);
    }

    private List<String> sentEventTypes() {
        return sent.stream().map(r -> new String(r.headers().lastHeader(OutboxHeaders.EVENT_TYPE).value(),
                StandardCharsets.UTF_8)).toList();
    }

    @Test
    @DisplayName("Kafka caído: reintenta con backoff, queda FAILED sin publicarse y tras recuperarlo sale con el mismo eventId")
    void retriesExhaustsAndRecovers() {
        OutboxEvent event = save("1", "OrderPaid", 1);
        kafkaUp.set(false);

        relay.relay();                                   // fallo 1 → espera 1 s
        assertThat(row(event.getId())).containsEntry("STATUS", "PENDING").containsEntry("RETRY_COUNT", 1);

        relay.relay();                                   // aún en backoff: no se intenta
        assertThat(row(event.getId())).containsEntry("RETRY_COUNT", 1);

        clock.advance(Duration.ofSeconds(1));
        relay.relay();                                   // fallo 2 → espera 2 s
        clock.advance(Duration.ofSeconds(2));
        relay.relay();                                   // fallo 3 = max-retries → FAILED

        assertThat(row(event.getId())).containsEntry("STATUS", "FAILED").containsEntry("PROCESSED", false)
                .containsEntry("RETRY_COUNT", 3);
        assertThat(row(event.getId()).get("FAILED_AT")).isNotNull();

        kafkaUp.set(true);
        clock.advance(Duration.ofHours(1));
        relay.relay();                                   // FAILED no se reintenta solo
        assertThat(sent).isEmpty();

        assertThat(recovery.requeue(event.getEventId())).isTrue();
        relay.relay();

        assertThat(row(event.getId())).containsEntry("STATUS", "PUBLISHED").containsEntry("PROCESSED", true);
        assertThat(sent).hasSize(1);
        assertThat(new String(sent.get(0).headers().lastHeader(OutboxHeaders.EVENT_ID).value(), StandardCharsets.UTF_8))
                .isEqualTo(event.getEventId().toString());
    }

    @Test
    @DisplayName("orden: los eventos de un agregado salen en orden aunque el primero falle; los de otro agregado no esperan")
    void keepsOrderPerAggregateAndLetsOthersAdvance() {
        OutboxEvent held = save("A", "SeatsHeld", 3);
        OutboxEvent paid = save("A", "OrderPaid", 2);
        OutboxEvent other = save("B", "OrderPaid", 1);

        kafkaUp.set(false);
        relay.relay();                                   // falla SeatsHeld de A y se corta el lote
        kafkaUp.set(true);
        relay.relay();                                   // A en backoff: solo sale B

        assertThat(row(other.getId())).containsEntry("STATUS", "PUBLISHED");
        assertThat(row(held.getId())).containsEntry("STATUS", "PENDING");
        assertThat(row(paid.getId())).containsEntry("STATUS", "PENDING");

        clock.advance(Duration.ofSeconds(1));
        relay.relay();                                   // sale SeatsHeld de A
        relay.relay();                                   // y solo después OrderPaid de A

        assertThat(sentEventTypes()).containsExactly("OrderPaid", "SeatsHeld", "OrderPaid");
        assertThat(sent).extracting(ProducerRecord::key).containsExactly("B", "A", "A");
        assertThat(row(paid.getId())).containsEntry("STATUS", "PUBLISHED");
    }

    @Test
    @DisplayName("orden: un FAILED retiene a los posteriores de su agregado hasta que se recupera")
    void failedEventHoldsItsAggregateUntilRecovered() {
        ReflectionTestUtils.setField(relay, "maxRetries", 1);
        OutboxEvent held = save("A", "SeatsHeld", 2);
        OutboxEvent paid = save("A", "OrderPaid", 1);

        kafkaUp.set(false);
        relay.relay();                                   // primer fallo = agotado
        kafkaUp.set(true);
        clock.advance(Duration.ofHours(1));
        relay.relay();

        assertThat(row(held.getId())).containsEntry("STATUS", "FAILED");
        assertThat(row(paid.getId())).containsEntry("STATUS", "PENDING");
        assertThat(sent).isEmpty();

        recovery.requeue(held.getEventId());
        relay.relay();
        relay.relay();

        assertThat(sentEventTypes()).containsExactly("SeatsHeld", "OrderPaid");
    }

    @Test
    @DisplayName("recuperar un evento que no está FAILED no cambia nada")
    void requeueOfNonFailedIsNoop() {
        OutboxEvent event = save("1", "OrderPaid", 1);

        assertThat(recovery.requeue(event.getEventId())).isFalse();
        assertThat(recovery.requeue(UUID.randomUUID())).isFalse();
        assertThat(row(event.getId())).containsEntry("STATUS", "PENDING").containsEntry("RETRY_COUNT", 0);
    }

    /** Reloj fijo que el test adelanta a mano. */
    private static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
