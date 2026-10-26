package com.capysoft.tuevento.shared.infrastructure.outbox;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

    @Mock OutboxEventRepository outboxEventRepository;
    @Mock KafkaTemplate<String, String> kafkaTemplate;
    @InjectMocks OutboxRelayScheduler relay;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(relay, "enabled", true);
        ReflectionTestUtils.setField(relay, "batchSize", 100);
        ReflectionTestUtils.setField(relay, "maxRetries", 5);
        ReflectionTestUtils.setField(relay, "sendTimeoutMs", 1000L);
    }

    private static OutboxEvent event(long id, int retries) {
        return OutboxEvent.builder().id(id).aggregateType("order").aggregateId("9").eventType("OrderPaid")
                .topic("tuevento.order.order-paid").payload("{}").retryCount(retries).build();
    }

    @Test
    @DisplayName("marca el evento como procesado cuando Kafka confirma el envío")
    void marksProcessedAfterAck() {
        when(outboxEventRepository.findPendingBatch(100)).thenReturn(List.of(event(1, 0)));
        when(kafkaTemplate.send("tuevento.order.order-paid", "9", "{}"))
                .thenReturn(CompletableFuture.completedFuture((SendResult<String, String>) null));

        relay.relay();

        verify(outboxEventRepository).markProcessed(1L);
        verify(outboxEventRepository, never()).markFailed(eq(1L), anyString());
    }

    @Test
    @DisplayName("si Kafka falla, suma un reintento y no lo marca como procesado")
    void marksFailedOnError() {
        when(outboxEventRepository.findPendingBatch(100)).thenReturn(List.of(event(2, 0)));
        when(kafkaTemplate.send("tuevento.order.order-paid", "9", "{}"))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker down")));

        relay.relay();

        verify(outboxEventRepository).markFailed(eq(2L), anyString());
        verify(outboxEventRepository, never()).markProcessed(2L);
    }

    @Test
    @DisplayName("un evento que agotó los reintentos se aparta sin enviarlo")
    void skipsExhaustedEvents() {
        when(outboxEventRepository.findPendingBatch(100)).thenReturn(List.of(event(3, 5)));

        relay.relay();

        verifyNoInteractions(kafkaTemplate);
        verify(outboxEventRepository).markProcessed(3L);
    }
}
