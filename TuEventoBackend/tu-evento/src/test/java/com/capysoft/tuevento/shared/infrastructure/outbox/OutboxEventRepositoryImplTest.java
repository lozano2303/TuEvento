package com.capysoft.tuevento.shared.infrastructure.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.capysoft.tuevento.shared.domain.outbox.OutboxEvent;
import com.capysoft.tuevento.shared.domain.outbox.OutboxStatus;

/**
 * Outbox contra una base real (H2 del perfil test): guardado con eventId, lote listo en orden
 * FIFO con orden por agregado y backoff, y transiciones PUBLISHED, FAILED y recuperación.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OutboxEventRepositoryImpl.class)
@DisplayName("OutboxEventRepositoryImpl")
class OutboxEventRepositoryImplTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 27, 10, 0);

    @Autowired OutboxEventRepositoryImpl repository;
    @Autowired JdbcTemplate jdbcTemplate;

    private OutboxEvent save(String aggregateId, LocalDateTime createdAt) {
        return save("order", aggregateId, createdAt, null);
    }

    private OutboxEvent save(String aggregateType, String aggregateId, LocalDateTime createdAt, UUID eventId) {
        return repository.save(OutboxEvent.builder().eventId(eventId).aggregateType(aggregateType).aggregateId(aggregateId)
                .eventType("OrderPaid").topic("tuevento.order").payload("{\"orderId\":" + aggregateId + "}")
                .createdAt(createdAt).build());
    }

    private Map<String, Object> row(Long id) {
        return jdbcTemplate.queryForMap("""
                SELECT event_id, status, processed, processed_at, retry_count, last_error,
                       next_attempt_at, last_attempt_at, failed_at, payload, topic
                FROM outbox_event WHERE id = ?""", id);
    }

    private java.util.List<Long> readyIds() {
        return repository.findPendingBatch(100, NOW).stream().map(OutboxEvent::getId).toList();
    }

    @Test
    @DisplayName("guarda PENDING con eventId y el lote sale en orden de creación, limitado al tamaño pedido")
    void savesPendingAndReturnsFifoBatch() {
        // Fechas fijas y separadas (reloj fijo NOW): ningún evento recibe now() al guardarse, así que
        // el orden no puede empatar por la resolución del reloj del sistema
        OutboxEvent later = save("2", NOW.minusMinutes(1));
        OutboxEvent first = save("1", NOW.minusMinutes(5));
        save("3", NOW.minusSeconds(1));

        assertThat(later.getId()).isNotNull();
        assertThat(later.getEventId()).isNotNull();
        assertThat(later.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(later.isProcessed()).isFalse();
        assertThat(later.getRetryCount()).isZero();
        assertThat(repository.findPendingBatch(2, NOW)).extracting(OutboxEvent::getId)
                .containsExactly(first.getId(), later.getId());
    }

    @Test
    @DisplayName("cada evento recibe un eventId distinto y se conserva el que trae quien publica")
    void assignsUniqueEventIdsAndKeepsGivenOne() {
        UUID given = UUID.randomUUID();
        OutboxEvent a = save("order", "1", NOW, null);
        OutboxEvent b = save("order", "2", NOW, null);
        OutboxEvent c = save("order", "3", NOW, given);

        assertThat(a.getEventId()).isNotEqualTo(b.getEventId());
        assertThat(c.getEventId()).isEqualTo(given);
        assertThat(row(c.getId()).get("EVENT_ID")).isEqualTo(given);
    }

    @Test
    @DisplayName("un eventId repetido se rechaza: el mismo evento de negocio no se guarda dos veces")
    void rejectsDuplicateEventId() {
        UUID eventId = UUID.randomUUID();
        save("order", "1", NOW, eventId);

        assertThatThrownBy(() -> save("order", "1", NOW, eventId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("publicado: sale del lote, queda PUBLISHED con processed y su fecha")
    void markPublishedRemovesFromBatch() {
        OutboxEvent event = save("1", NOW.minusMinutes(1));

        repository.markPublished(event.getId(), NOW);

        Map<String, Object> row = row(event.getId());
        assertThat(row).containsEntry("STATUS", "PUBLISHED").containsEntry("PROCESSED", true);
        assertThat(row.get("PROCESSED_AT")).isNotNull();
        assertThat(row.get("LAST_ATTEMPT_AT")).isNotNull();
        assertThat(readyIds()).isEmpty();
    }

    @Test
    @DisplayName("reintento: suma uno, guarda el error recortado y no sale del lote hasta cumplir su backoff")
    void scheduleRetryHonoursBackoff() {
        OutboxEvent event = save("1", NOW.minusMinutes(1));

        repository.scheduleRetry(event.getId(), "x".repeat(1500), NOW, NOW.plusSeconds(30));

        Map<String, Object> row = row(event.getId());
        assertThat(row).containsEntry("STATUS", "PENDING").containsEntry("PROCESSED", false)
                .containsEntry("RETRY_COUNT", 1);
        assertThat((String) row.get("LAST_ERROR")).hasSize(1000);
        assertThat(repository.findPendingBatch(100, NOW.plusSeconds(29))).isEmpty();
        assertThat(repository.findPendingBatch(100, NOW.plusSeconds(30))).extracting(OutboxEvent::getId)
                .containsExactly(event.getId());

        repository.scheduleRetry(event.getId(), null, NOW, NOW);
        assertThat(row(event.getId())).containsEntry("RETRY_COUNT", 2);
        assertThat(row(event.getId()).get("LAST_ERROR")).isNull();
    }

    @Test
    @DisplayName("agotado: queda FAILED sin marcarse procesado y conserva payload, topic, error y fechas")
    void markExhaustedKeepsEverythingForInvestigation() {
        OutboxEvent event = save("1", NOW.minusMinutes(1));

        repository.markExhausted(event.getId(), "broker down", NOW);

        Map<String, Object> row = row(event.getId());
        assertThat(row).containsEntry("STATUS", "FAILED").containsEntry("PROCESSED", false)
                .containsEntry("RETRY_COUNT", 1).containsEntry("LAST_ERROR", "broker down")
                .containsEntry("PAYLOAD", "{\"orderId\":1}").containsEntry("TOPIC", "tuevento.order")
                .containsEntry("EVENT_ID", event.getEventId());
        assertThat(row.get("PROCESSED_AT")).isNull();
        assertThat(row.get("FAILED_AT")).isNotNull();
        assertThat(readyIds()).isEmpty();
    }

    @Test
    @DisplayName("recuperación: un FAILED vuelve a PENDING con el contador a cero, el mismo eventId y su último error")
    void requeueFailedReturnsToPending() {
        OutboxEvent event = save("1", NOW.minusMinutes(1));
        repository.markExhausted(event.getId(), "broker down", NOW.minusMinutes(1));

        assertThat(repository.requeueFailed(event.getEventId(), NOW)).isTrue();

        Map<String, Object> row = row(event.getId());
        assertThat(row).containsEntry("STATUS", "PENDING").containsEntry("RETRY_COUNT", 0)
                .containsEntry("EVENT_ID", event.getEventId()).containsEntry("LAST_ERROR", "broker down");
        assertThat(row.get("FAILED_AT")).isNull();
        assertThat(readyIds()).containsExactly(event.getId());
    }

    @Test
    @DisplayName("recuperación: no toca eventos PENDING, PUBLISHED ni inexistentes")
    void requeueIgnoresNonFailed() {
        OutboxEvent pending = save("1", NOW.minusMinutes(2));
        OutboxEvent published = save("2", NOW.minusMinutes(1));
        repository.markPublished(published.getId(), NOW);

        assertThat(repository.requeueFailed(pending.getEventId(), NOW)).isFalse();
        assertThat(repository.requeueFailed(published.getEventId(), NOW)).isFalse();
        assertThat(repository.requeueFailed(UUID.randomUUID(), NOW)).isFalse();
        assertThat(row(published.getId())).containsEntry("STATUS", "PUBLISHED");
    }

    @Test
    @DisplayName("orden: un evento PENDING retiene a los posteriores de su agregado, no a los de otros")
    void pendingEventBlocksLaterEventsOfSameAggregateOnly() {
        OutboxEvent a1 = save("order", "A", NOW.minusMinutes(3), null);
        OutboxEvent a2 = save("order", "A", NOW.minusMinutes(2), null);
        OutboxEvent b1 = save("order", "B", NOW.minusMinutes(1), null);

        assertThat(readyIds()).containsExactly(a1.getId(), b1.getId()).doesNotContain(a2.getId());

        repository.markPublished(a1.getId(), NOW);
        assertThat(readyIds()).containsExactly(a2.getId(), b1.getId());
    }

    @Test
    @DisplayName("orden: un evento en backoff no sale y tampoco deja salir a los posteriores de su agregado")
    void eventInBackoffBlocksItsAggregate() {
        OutboxEvent a1 = save("order", "A", NOW.minusMinutes(3), null);
        OutboxEvent a2 = save("order", "A", NOW.minusMinutes(2), null);
        OutboxEvent b1 = save("order", "B", NOW.minusMinutes(1), null);

        repository.scheduleRetry(a1.getId(), "broker down", NOW, NOW.plusMinutes(5));

        assertThat(readyIds()).containsExactly(b1.getId()).doesNotContain(a1.getId(), a2.getId());
    }

    @Test
    @DisplayName("orden: un evento FAILED retiene a su agregado hasta recuperarlo; los demás agregados avanzan")
    void failedEventBlocksItsAggregateUntilRequeued() {
        OutboxEvent a1 = save("order", "A", NOW.minusMinutes(3), null);
        OutboxEvent a2 = save("order", "A", NOW.minusMinutes(2), null);
        OutboxEvent b1 = save("order", "B", NOW.minusMinutes(1), null);

        repository.markExhausted(a1.getId(), "broker down", NOW);
        assertThat(readyIds()).containsExactly(b1.getId());

        repository.requeueFailed(a1.getEventId(), NOW);
        assertThat(readyIds()).containsExactly(a1.getId(), b1.getId()).doesNotContain(a2.getId());
    }

    @Test
    @DisplayName("orden: el mismo aggregateId en otro tipo de agregado no se bloquea")
    void aggregateIsTypeAndId() {
        OutboxEvent order9 = save("order", "9", NOW.minusMinutes(2), null);
        OutboxEvent seat9 = save("seat", "9", NOW.minusMinutes(1), null);

        repository.markExhausted(order9.getId(), "broker down", NOW);

        assertThat(readyIds()).containsExactly(seat9.getId());
    }

    @Test
    @DisplayName("orden: con la misma fecha de creación desempata el id")
    void sameCreatedAtUsesIdOrder() {
        OutboxEvent a1 = save("order", "A", NOW.minusMinutes(1), null);
        OutboxEvent a2 = save("order", "A", NOW.minusMinutes(1), null);

        assertThat(readyIds()).containsExactly(a1.getId()).doesNotContain(a2.getId());
    }

    @Test
    @DisplayName("un evento PUBLISHED anterior no bloquea a los siguientes de su agregado")
    void publishedEventDoesNotBlock() {
        OutboxEvent a1 = save("order", "A", NOW.minusMinutes(2), null);
        OutboxEvent a2 = save("order", "A", NOW.minusMinutes(1), null);
        repository.markPublished(a1.getId(), NOW);

        assertThat(readyIds()).containsExactly(a2.getId());
    }
}
