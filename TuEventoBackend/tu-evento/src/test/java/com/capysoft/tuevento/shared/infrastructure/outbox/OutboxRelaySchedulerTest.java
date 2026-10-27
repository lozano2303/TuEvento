package com.capysoft.tuevento.shared.infrastructure.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import com.capysoft.tuevento.shared.domain.outbox.OutboxEvent;
import com.capysoft.tuevento.shared.domain.outbox.OutboxEventRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxRelayScheduler")
class OutboxRelaySchedulerTest {

    private static final Instant INSTANT = Instant.parse("2026-10-27T15:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(INSTANT, ZoneId.systemDefault());

    @Mock OutboxEventRepository outboxEventRepository;
    @Mock KafkaTemplate<String, String> kafkaTemplate;
    OutboxRelayScheduler relay;

    @BeforeEach
    void setUp() {
        // La zona del Clock no importa: el relay toma el instante y lo pasa a la zona de la JVM
        relay = new OutboxRelayScheduler(outboxEventRepository, kafkaTemplate, Clock.fixed(INSTANT, ZoneOffset.ofHours(-5)));
        ReflectionTestUtils.setField(relay, "enabled", true);
        ReflectionTestUtils.setField(relay, "batchSize", 100);
        ReflectionTestUtils.setField(relay, "maxRetries", 5);
        ReflectionTestUtils.setField(relay, "backoffInitialMs", 1000L);
        ReflectionTestUtils.setField(relay, "backoffMaxMs", 60000L);
        ReflectionTestUtils.setField(relay, "sendTimeoutMs", 1000L);
    }

    private static final UUID EVENT_ID = UUID.fromString("6f1c2a54-0d7e-4b8a-9d3e-2c1b0a9f8e7d");

    private static OutboxEvent event(long id, int retries) {
        return event(id, "9", retries);
    }

    private static OutboxEvent event(long id, String aggregateId, int retries) {
        return OutboxEvent.builder().id(id).eventId(id == 1 ? EVENT_ID : UUID.randomUUID())
                .aggregateType("order").aggregateId(aggregateId).eventType("OrderPaid")
                .topic("tuevento.order").payload("{}").retryCount(retries)
                .createdAt(LocalDateTime.of(2026, 10, 27, 9, 59)).build();
    }

    private void kafkaAcks() {
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture((SendResult<String, String>) null));
    }

    private void kafkaFails() {
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker down")));
    }

    private static String header(ProducerRecord<String, String> rec, String name) {
        return new String(rec.headers().lastHeader(name).value(), StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private List<ProducerRecord<String, String>> sentRecords(int times) {
        ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate, times(times)).send(captor.capture());
        return captor.getAllValues();
    }

    @Test
    @DisplayName("publica con la key del agregado, el payload intacto y los headers del evento")
    void publishesWithHeadersAndMarksPublished() {
        when(outboxEventRepository.findPendingBatch(100, NOW)).thenReturn(List.of(event(1, 0)));
        kafkaAcks();

        relay.relay();

        ProducerRecord<String, String> rec = sentRecords(1).get(0);
        assertThat(rec.topic()).isEqualTo("tuevento.order");
        assertThat(rec.key()).isEqualTo("9");
        assertThat(rec.value()).isEqualTo("{}");
        assertThat(header(rec, OutboxHeaders.EVENT_ID)).isEqualTo(EVENT_ID.toString());
        assertThat(header(rec, OutboxHeaders.EVENT_TYPE)).isEqualTo("OrderPaid");
        assertThat(header(rec, OutboxHeaders.AGGREGATE_TYPE)).isEqualTo("order");
        assertThat(header(rec, OutboxHeaders.AGGREGATE_ID)).isEqualTo("9");
        assertThat(header(rec, OutboxHeaders.SCHEMA_VERSION)).isEqualTo("1");
        assertThat(header(rec, OutboxHeaders.OCCURRED_AT)).isEqualTo(
                LocalDateTime.of(2026, 10, 27, 9, 59).atZone(ZoneId.systemDefault()).toInstant().toString());
        assertThat(Instant.parse(header(rec, OutboxHeaders.OCCURRED_AT))).isNotNull();
        verify(outboxEventRepository).markPublished(1L, NOW);
        verify(outboxEventRepository, never()).scheduleRetry(any(), any(), any(), any());
        verify(outboxEventRepository, never()).markExhausted(any(), any(), any());
    }

    @Test
    @DisplayName("si Kafka falla, suma un reintento con backoff y no lo marca como publicado")
    void schedulesRetryWithBackoffOnError() {
        when(outboxEventRepository.findPendingBatch(100, NOW)).thenReturn(List.of(event(2, 0)));
        kafkaFails();

        relay.relay();

        verify(outboxEventRepository).scheduleRetry(eq(2L), anyString(), eq(NOW), eq(NOW.plusSeconds(1)));
        verify(outboxEventRepository, never()).markPublished(any(), any());
        verify(outboxEventRepository, never()).markExhausted(any(), any(), any());
    }

    @Test
    @DisplayName("el backoff se duplica en cada fallo hasta el tope configurado")
    void backoffDoublesUpToMax() {
        assertThat(relay.backoff(1)).isEqualTo(Duration.ofSeconds(1));
        assertThat(relay.backoff(2)).isEqualTo(Duration.ofSeconds(2));
        assertThat(relay.backoff(3)).isEqualTo(Duration.ofSeconds(4));
        assertThat(relay.backoff(6)).isEqualTo(Duration.ofSeconds(32));
        assertThat(relay.backoff(7)).isEqualTo(Duration.ofSeconds(60));
        assertThat(relay.backoff(500)).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("el tercer fallo programa el siguiente intento a 4 s")
    void thirdFailureWaitsFourSeconds() {
        when(outboxEventRepository.findPendingBatch(100, NOW)).thenReturn(List.of(event(2, 2)));
        kafkaFails();

        relay.relay();

        verify(outboxEventRepository).scheduleRetry(eq(2L), anyString(), eq(NOW), eq(NOW.plusSeconds(4)));
    }

    @Test
    @DisplayName("el fallo número max-retries deja el evento FAILED, nunca publicado")
    void lastFailureMarksExhausted() {
        when(outboxEventRepository.findPendingBatch(100, NOW)).thenReturn(List.of(event(3, 4)));
        kafkaFails();

        relay.relay();

        verify(outboxEventRepository).markExhausted(eq(3L), anyString(), eq(NOW));
        verify(outboxEventRepository, never()).markPublished(any(), any());
        verify(outboxEventRepository, never()).scheduleRetry(any(), any(), any(), any());
    }

    @Test
    @DisplayName("compatibilidad: un evento que ya superaba max-retries se intenta una vez más en vez de descartarse")
    void eventAboveMaxRetriesIsStillAttempted() {
        when(outboxEventRepository.findPendingBatch(100, NOW)).thenReturn(List.of(event(4, 7)));
        kafkaAcks();

        relay.relay();

        verify(outboxEventRepository).markPublished(4L, NOW);
        verify(outboxEventRepository, never()).markExhausted(any(), any(), any());
    }

    @Test
    @DisplayName("ante un fallo el lote se corta: los siguientes no se envían ni gastan reintentos")
    void stopsBatchOnFirstFailure() {
        when(outboxEventRepository.findPendingBatch(100, NOW))
                .thenReturn(List.of(event(5, "1", 0), event(6, "2", 0), event(7, "3", 0)));
        kafkaFails();

        relay.relay();

        sentRecords(1);
        verify(outboxEventRepository).scheduleRetry(eq(5L), anyString(), eq(NOW), any());
        verify(outboxEventRepository, never()).scheduleRetry(eq(6L), any(), any(), any());
        verify(outboxEventRepository, never()).scheduleRetry(eq(7L), any(), any(), any());
        verify(outboxEventRepository, never()).markPublished(any(), any());
    }

    @Test
    @DisplayName("duplicado: si falla la marca tras el envío, la ejecución se deshace y el reenvío lleva el mismo eventId")
    void resendAfterFailedMarkKeepsEventId() {
        when(outboxEventRepository.findPendingBatch(100, NOW)).thenReturn(List.of(event(1, 0)));
        kafkaAcks();
        doThrow(new IllegalStateException("commit lost")).doNothing()
                .when(outboxEventRepository).markPublished(1L, NOW);

        assertThatThrownBy(() -> relay.relay()).isInstanceOf(IllegalStateException.class);
        relay.relay();

        List<ProducerRecord<String, String>> sent = sentRecords(2);
        assertThat(header(sent.get(0), OutboxHeaders.EVENT_ID))
                .isEqualTo(header(sent.get(1), OutboxHeaders.EVENT_ID))
                .isEqualTo(EVENT_ID.toString());
        // El fallo de la marca no es un fallo de Kafka: no gasta reintentos
        verify(outboxEventRepository, never()).scheduleRetry(any(), any(), any(), any());
        verify(outboxEventRepository, times(2)).markPublished(1L, NOW);
    }

    @Test
    @DisplayName("una interrupción cuenta como fallo y conserva el estado de interrupción del hilo")
    void interruptionCountsAsFailure() {
        when(outboxEventRepository.findPendingBatch(100, NOW)).thenReturn(List.of(event(8, 0)));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenAnswer(inv -> {
            Thread.currentThread().interrupt();
            return new CompletableFuture<SendResult<String, String>>();
        });

        try {
            relay.relay();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
        verify(outboxEventRepository).scheduleRetry(eq(8L), eq("interrupted"), eq(NOW), eq(NOW.plusSeconds(1)));
    }

    @Test
    @DisplayName("la configuración inválida se rechaza al arrancar")
    void rejectsInvalidConfig() {
        relay.validateConfig();

        ReflectionTestUtils.setField(relay, "maxRetries", 0);
        assertThatThrownBy(relay::validateConfig).isInstanceOf(IllegalStateException.class);
        ReflectionTestUtils.setField(relay, "maxRetries", 10);

        ReflectionTestUtils.setField(relay, "backoffInitialMs", 0L);
        assertThatThrownBy(relay::validateConfig).isInstanceOf(IllegalStateException.class);
        ReflectionTestUtils.setField(relay, "backoffInitialMs", 120000L);
        assertThatThrownBy(relay::validateConfig).isInstanceOf(IllegalStateException.class);
        ReflectionTestUtils.setField(relay, "backoffInitialMs", 1000L);

        ReflectionTestUtils.setField(relay, "batchSize", 0);
        assertThatThrownBy(relay::validateConfig).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("sin eventos listos no envía nada")
    void emptyBatchDoesNothing() {
        when(outboxEventRepository.findPendingBatch(100, NOW)).thenReturn(List.of());

        relay.relay();

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("deshabilitado no lee el outbox")
    void disabledDoesNothing() {
        ReflectionTestUtils.setField(relay, "enabled", false);

        relay.relay();

        verifyNoInteractions(outboxEventRepository, kafkaTemplate);
    }
}
