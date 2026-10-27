package com.capysoft.tuevento.shared.infrastructure.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.capysoft.tuevento.shared.domain.outbox.OutboxEvent;

/**
 * Outbox contra una base real (H2 del perfil test): guardado, lote pendiente en orden FIFO y
 * marcas de procesado y de fallo.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OutboxEventRepositoryImpl.class)
@DisplayName("OutboxEventRepositoryImpl")
class OutboxEventRepositoryImplTest {

    @Autowired OutboxEventRepositoryImpl repository;
    @Autowired JdbcTemplate jdbcTemplate;

    private OutboxEvent save(String aggregateId, LocalDateTime createdAt) {
        return repository.save(OutboxEvent.builder().aggregateType("order").aggregateId(aggregateId)
                .eventType("OrderPaid").topic("tuevento.order.order-paid").payload("{\"orderId\":" + aggregateId + "}")
                .createdAt(createdAt).build());
    }

    private Map<String, Object> row(Long id) {
        return jdbcTemplate.queryForMap("SELECT processed, processed_at, retry_count, last_error FROM outbox_event WHERE id = ?", id);
    }

    @Test
    @DisplayName("guarda pendiente y el lote sale en orden de creación, limitado al tamaño pedido")
    void savesPendingAndReturnsFifoBatch() {
        // Fechas separadas: el evento sin fecha recibe now() al guardarse y, con la resolución del
        // reloj de Windows, otro now() tomado justo antes puede salir idéntico y empatar en el orden
        OutboxEvent later = save("2", LocalDateTime.now().minusMinutes(1));
        OutboxEvent first = save("1", LocalDateTime.now().minusMinutes(5));
        save("3", null);

        assertThat(later.getId()).isNotNull();
        assertThat(later.isProcessed()).isFalse();
        assertThat(later.getRetryCount()).isZero();
        assertThat(repository.findPendingBatch(2)).extracting(OutboxEvent::getId)
                .containsExactly(first.getId(), later.getId());
    }

    @Test
    @DisplayName("un evento procesado sale del lote y queda con su fecha")
    void markProcessedRemovesFromBatch() {
        OutboxEvent event = save("1", LocalDateTime.now());

        repository.markProcessed(event.getId());

        assertThat(row(event.getId())).containsEntry("PROCESSED", true);
        assertThat(row(event.getId()).get("PROCESSED_AT")).isNotNull();
    }

    @Test
    @DisplayName("un fallo suma un reintento y guarda el error, recortado a 1000 caracteres")
    void markFailedCountsRetryAndTruncatesError() {
        OutboxEvent event = save("1", LocalDateTime.now());

        repository.markFailed(event.getId(), "x".repeat(1500));
        repository.markFailed(event.getId(), null);

        Map<String, Object> row = row(event.getId());
        assertThat(row).containsEntry("PROCESSED", false).containsEntry("RETRY_COUNT", 2);
        assertThat(row.get("LAST_ERROR")).isNull();

        repository.markFailed(event.getId(), "x".repeat(1500));
        assertThat((String) row(event.getId()).get("LAST_ERROR")).hasSize(1000);
    }
}
