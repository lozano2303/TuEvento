package com.capysoft.tuevento.modules.event.infrastructure.messaging;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import com.capysoft.tuevento.modules.event.application.port.in.CloseRevokedOrganizerEventsPort;
import com.capysoft.tuevento.shared.domain.outbox.OutboxEvent;
import com.capysoft.tuevento.shared.domain.outbox.OutboxEventRepository;
import com.capysoft.tuevento.shared.domain.outbox.OutboxStatus;
import com.capysoft.tuevento.shared.infrastructure.config.DomainEventsConsumerConfig;
import com.capysoft.tuevento.shared.infrastructure.config.KafkaConfig;
import com.capysoft.tuevento.shared.infrastructure.messaging.ProcessedEventStore;
import com.capysoft.tuevento.shared.infrastructure.outbox.OutboxRelayScheduler;

/**
 * De extremo a extremo con un broker Kafka embebido: el relay del outbox publica
 * {@code OrganizerRoleRevoked} con sus headers y el contenedor de Catálogo lo entrega al listener,
 * reintentando si el cierre de eventos falla.
 */
@SpringBootTest(classes = {KafkaConfig.class, DomainEventsConsumerConfig.class, OrganizerRoleRevokedKafkaListener.class},
        properties = {
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.kafka.consumer.auto-offset-reset=earliest",
                "messaging.consumers.enabled=true",
                "messaging.consumers.max-retry-ms=20000"
        })
@ImportAutoConfiguration({KafkaAutoConfiguration.class, JacksonAutoConfiguration.class})
@EmbeddedKafka(partitions = 1, topics = "tuevento.user")
@DisplayName("OrganizerRoleRevoked: outbox → Kafka → Catálogo")
class OrganizerRoleRevokedKafkaFlowTest {

    private static final String PAYLOAD = """
            {"revocationId":77,"userId":10,"alias":"john","revokedBy":1,\
            "reason":"Incumplimiento de las normas","occurredAt":"2026-10-27T15:00:00Z"}""";

    @Autowired KafkaTemplate<String, String> kafkaTemplate;
    @MockitoBean CloseRevokedOrganizerEventsPort closePort;
    @MockitoBean ProcessedEventStore processedEventStore;
    @MockitoBean OutboxEventRepository outboxEventRepository;

    private OutboxRelayScheduler relay;

    @BeforeEach
    void setUp() {
        relay = new OutboxRelayScheduler(outboxEventRepository, kafkaTemplate, Clock.systemUTC());
        ReflectionTestUtils.setField(relay, "enabled", true);
        ReflectionTestUtils.setField(relay, "batchSize", 100);
        ReflectionTestUtils.setField(relay, "maxRetries", 10);
        ReflectionTestUtils.setField(relay, "backoffInitialMs", 1000L);
        ReflectionTestUtils.setField(relay, "backoffMaxMs", 60000L);
        ReflectionTestUtils.setField(relay, "sendTimeoutMs", 10000L);
    }

    private UUID publishThroughRelay() {
        UUID eventId = UUID.randomUUID();
        when(outboxEventRepository.findPendingBatch(eq(100), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(OutboxEvent.builder().id(1L).eventId(eventId).aggregateType("user")
                        .aggregateId("10").eventType("OrganizerRoleRevoked").topic("tuevento.user")
                        .payload(PAYLOAD).status(OutboxStatus.PENDING).createdAt(LocalDateTime.now()).build()));
        relay.relay();
        verify(outboxEventRepository).markPublished(eq(1L), org.mockito.ArgumentMatchers.any());
        return eventId;
    }

    @Test
    @DisplayName("el mensaje llega con sus headers y Catálogo cierra los eventos y lo marca procesado")
    void deliversAndProcesses() {
        UUID eventId = publishThroughRelay();

        verify(closePort, timeout(30_000)).close(10, 1, "Incumplimiento de las normas");
        verify(processedEventStore, timeout(10_000)).markProcessed(OrganizerRoleRevokedKafkaListener.CONSUMER, eventId);
    }

    @Test
    @DisplayName("si el cierre falla, el contenedor lo reintenta con backoff hasta que funciona")
    void retriesUntilSuccess() {
        when(closePort.close(anyInt(), anyInt(), anyString()))
                .thenThrow(new IllegalStateException("db down"))
                .thenReturn(1);

        UUID eventId = publishThroughRelay();

        verify(closePort, timeout(30_000).times(2)).close(10, 1, "Incumplimiento de las normas");
        verify(processedEventStore, timeout(10_000).times(1))
                .markProcessed(OrganizerRoleRevokedKafkaListener.CONSUMER, eventId);
    }
}
