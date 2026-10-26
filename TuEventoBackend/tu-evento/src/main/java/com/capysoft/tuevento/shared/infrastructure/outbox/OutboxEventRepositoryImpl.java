package com.capysoft.tuevento.shared.infrastructure.outbox;

import com.capysoft.tuevento.shared.domain.outbox.OutboxEvent;
import com.capysoft.tuevento.shared.domain.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OutboxEventRepositoryImpl implements OutboxEventRepository {

    private final OutboxEventJpaRepository jpaRepository;

    @Override
    public OutboxEvent save(OutboxEvent event) {
        OutboxEventEntity entity = OutboxEventEntity.builder()
                .aggregateType(event.getAggregateType())
                .aggregateId(event.getAggregateId())
                .eventType(event.getEventType())
                .payload(event.getPayload())
                .topic(event.getTopic())
                .processed(false)
                .createdAt(event.getCreatedAt() != null ? event.getCreatedAt() : LocalDateTime.now())
                .retryCount(0)
                .build();
        OutboxEventEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<OutboxEvent> findPendingBatch(int limit) {
        return jpaRepository
                .findByProcessedFalseOrderByCreatedAtAsc(PageRequest.of(0, limit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void markProcessed(Long id) {
        jpaRepository.markProcessed(id, LocalDateTime.now());
    }

    @Override
    public void markFailed(Long id, String errorMessage) {
        // Truncar a 1000 chars para no desbordar la columna TEXT en casos extremos
        String truncated = errorMessage != null && errorMessage.length() > 1000
                ? errorMessage.substring(0, 1000)
                : errorMessage;
        jpaRepository.markFailed(id, truncated);
    }

    private OutboxEvent toDomain(OutboxEventEntity entity) {
        return OutboxEvent.builder()
                .id(entity.getId())
                .aggregateType(entity.getAggregateType())
                .aggregateId(entity.getAggregateId())
                .eventType(entity.getEventType())
                .payload(entity.getPayload())
                .topic(entity.getTopic())
                .processed(entity.isProcessed())
                .createdAt(entity.getCreatedAt())
                .processedAt(entity.getProcessedAt())
                .retryCount(entity.getRetryCount())
                .lastError(entity.getLastError())
                .build();
    }
}
