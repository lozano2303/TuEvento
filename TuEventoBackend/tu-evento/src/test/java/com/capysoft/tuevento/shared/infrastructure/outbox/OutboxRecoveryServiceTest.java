package com.capysoft.tuevento.shared.infrastructure.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.capysoft.tuevento.shared.domain.outbox.OutboxEventRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxRecoveryService")
class OutboxRecoveryServiceTest {

    private static final Instant INSTANT = Instant.parse("2026-10-27T15:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(INSTANT, ZoneId.systemDefault());

    @Mock OutboxEventRepository outboxEventRepository;

    private OutboxRecoveryService service() {
        return new OutboxRecoveryService(outboxEventRepository, Clock.fixed(INSTANT, ZoneOffset.ofHours(-5)));
    }

    @Test
    @DisplayName("devuelve true cuando el evento FAILED vuelve a la cola, con la hora de la JVM")
    void requeuesFailedEvent() {
        UUID eventId = UUID.randomUUID();
        when(outboxEventRepository.requeueFailed(eventId, NOW)).thenReturn(true);

        assertThat(service().requeue(eventId)).isTrue();
        verify(outboxEventRepository).requeueFailed(eventId, NOW);
    }

    @Test
    @DisplayName("devuelve false si el evento no existe o no estaba FAILED")
    void reportsNothingToRequeue() {
        UUID eventId = UUID.randomUUID();
        when(outboxEventRepository.requeueFailed(eventId, NOW)).thenReturn(false);

        assertThat(service().requeue(eventId)).isFalse();
    }
}
