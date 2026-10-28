package com.capysoft.tuevento.shared.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Registro de mensajes procesados contra una base real (H2 del perfil test). */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ProcessedEventStore.class)
@DisplayName("ProcessedEventStore")
class ProcessedEventStoreTest {

    @Autowired ProcessedEventStore store;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("recuerda el mensaje por consumidor y marcarlo dos veces no falla ni duplica")
    void remembersPerConsumer() {
        UUID eventId = UUID.randomUUID();

        assertThat(store.isProcessed("catalogo", eventId)).isFalse();
        store.markProcessed("catalogo", eventId);
        store.markProcessed("catalogo", eventId);

        assertThat(store.isProcessed("catalogo", eventId)).isTrue();
        assertThat(store.isProcessed("comercio", eventId)).isFalse();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM processed_event", Integer.class)).isEqualTo(1);
    }
}
