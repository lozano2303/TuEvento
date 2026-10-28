package com.capysoft.tuevento.shared.infrastructure.outbox;

import com.capysoft.tuevento.shared.domain.outbox.OutboxEvent;
import com.capysoft.tuevento.shared.domain.outbox.OutboxEventRepository;
import com.capysoft.tuevento.shared.domain.outbox.OutboxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OutboxEventRepositoryImpl implements OutboxEventRepository {

    private static final int MAX_ERROR_LENGTH = 1000;

    private final OutboxEventJpaRepository jpaRepository;

    @Override
    public OutboxEvent save(OutboxEvent event) {
        OutboxEventEntity entity = OutboxEventEntity.builder()
                .eventId(event.getEventId() != null ? event.getEventId() : UUID.randomUUID())
                .aggregateType(event.getAggregateType())
                .aggregateId(event.getAggregateId())
                .eventType(event.getEventType())
                .payload(event.getPayload())
                .topic(event.getTopic())
                .status(OutboxStatus.PENDING)
                .processed(false)
                .createdAt(event.getCreatedAt() != null ? event.getCreatedAt() : LocalDateTime.now())
                .retryCount(0)
                .build();
        OutboxEventEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<OutboxEvent> findPendingBatch(int limit, LocalDateTime now) {
        return jpaRepository
                .findReadyBatch(now, PageRequest.of(0, limit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void markPublished(Long id, LocalDateTime now) {
        jpaRepository.markPublished(id, now, OutboxStatus.PUBLISHED);
    }

    @Override
    public void scheduleRetry(Long id, String errorMessage, LocalDateTime now, LocalDateTime nextAttemptAt) {
        jpaRepository.scheduleRetry(id, truncate(errorMessage), now, nextAttemptAt);
    }

    @Override
    public void markExhausted(Long id, String errorMessage, LocalDateTime now) {
        jpaRepository.markExhausted(id, truncate(errorMessage), now, OutboxStatus.FAILED);
    }

    @Override
    public boolean requeueFailed(UUID eventId, LocalDateTime now) {
        return jpaRepository.requeueFailed(eventId, now, OutboxStatus.PENDING, OutboxStatus.FAILED) > 0;
    }

    /** Recorta a 1000 caracteres para no guardar trazas enormes en casos extremos. */
    private static String truncate(String errorMessage) {
        return errorMessage != null && errorMessage.length() > MAX_ERROR_LENGTH
                ? errorMessage.substring(0, MAX_ERROR_LENGTH)
                : errorMessage;
    }

    private OutboxEvent toDomain(OutboxEventEntity entity) {
        return OutboxEvent.builder()
                .id(entity.getId())
                .eventId(entity.getEventId())
                .aggregateType(entity.getAggregateType())
                .aggregateId(entity.getAggregateId())
                .eventType(entity.getEventType())
                .payload(entity.getPayload())
                .topic(entity.getTopic())
                .status(entity.getStatus())
                .processed(entity.isProcessed())
                .createdAt(entity.getCreatedAt())
                .processedAt(entity.getProcessedAt())
                .retryCount(entity.getRetryCount())
                .lastError(entity.getLastError())
                .nextAttemptAt(entity.getNextAttemptAt())
                .lastAttemptAt(entity.getLastAttemptAt())
                .failedAt(entity.getFailedAt())
                .build();
    }
}
