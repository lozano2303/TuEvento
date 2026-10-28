package com.capysoft.tuevento.shared.infrastructure.outbox;

import com.capysoft.tuevento.shared.domain.outbox.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, Long> {

    /**
     * Batch de eventos listos para publicar, ordenados por createdAt (y id para desempatar) para FIFO.
     *
     * <p>Orden por agregado: se excluye un evento si su agregado tiene otro anterior en PENDING o
     * FAILED. El NOT EXISTS es una lectura normal, así que también ve las filas que otra réplica
     * tiene bloqueadas: nadie publica el segundo evento de un agregado mientras el primero sigue
     * sin confirmarse. Como mucho sale un evento por agregado en cada lote.
     *
     * <p>{@code SELECT ... FOR UPDATE SKIP LOCKED} (timeout -2 = SKIP_LOCKED en Hibernate): con
     * varias réplicas, cada una toma filas distintas y nadie publica el mismo evento a la vez.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("""
            SELECT e FROM OutboxEventEntity e
            WHERE e.status = com.capysoft.tuevento.shared.domain.outbox.OutboxStatus.PENDING
              AND (e.nextAttemptAt IS NULL OR e.nextAttemptAt <= :now)
              AND NOT EXISTS (
                  SELECT 1 FROM OutboxEventEntity p
                  WHERE p.aggregateType = e.aggregateType
                    AND p.aggregateId = e.aggregateId
                    AND p.status IN (com.capysoft.tuevento.shared.domain.outbox.OutboxStatus.PENDING,
                                     com.capysoft.tuevento.shared.domain.outbox.OutboxStatus.FAILED)
                    AND (p.createdAt < e.createdAt OR (p.createdAt = e.createdAt AND p.id < e.id)))
            ORDER BY e.createdAt ASC, e.id ASC
            """)
    List<OutboxEventEntity> findReadyBatch(@Param("now") LocalDateTime now, Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE OutboxEventEntity e
            SET e.status = :status, e.processed = true, e.processedAt = :now, e.lastAttemptAt = :now
            WHERE e.id = :id
            """)
    void markPublished(@Param("id") Long id, @Param("now") LocalDateTime now,
                       @Param("status") OutboxStatus status);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE OutboxEventEntity e
            SET e.retryCount = e.retryCount + 1, e.lastError = :error,
                e.lastAttemptAt = :now, e.nextAttemptAt = :nextAttemptAt
            WHERE e.id = :id
            """)
    void scheduleRetry(@Param("id") Long id, @Param("error") String error,
                       @Param("now") LocalDateTime now, @Param("nextAttemptAt") LocalDateTime nextAttemptAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE OutboxEventEntity e
            SET e.retryCount = e.retryCount + 1, e.lastError = :error, e.lastAttemptAt = :now,
                e.failedAt = :now, e.status = :status
            WHERE e.id = :id
            """)
    void markExhausted(@Param("id") Long id, @Param("error") String error,
                       @Param("now") LocalDateTime now, @Param("status") OutboxStatus status);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE OutboxEventEntity e
            SET e.status = :pending, e.retryCount = 0, e.nextAttemptAt = :now, e.failedAt = NULL
            WHERE e.eventId = :eventId AND e.status = :failed
            """)
    int requeueFailed(@Param("eventId") UUID eventId, @Param("now") LocalDateTime now,
                      @Param("pending") OutboxStatus pending, @Param("failed") OutboxStatus failed);
}
