package com.capysoft.tuevento.shared.infrastructure.outbox;

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

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, Long> {

    /**
     * Batch de eventos pendientes ordenados por createdAt ASC para FIFO.
     * Usado por el relay para publicar en orden de creación.
     *
     * <p>{@code SELECT ... FOR UPDATE SKIP LOCKED} (timeout -2 = SKIP_LOCKED en Hibernate): con
     * varias réplicas, cada una toma filas distintas y nadie publica el mismo evento a la vez.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    List<OutboxEventEntity> findByProcessedFalseOrderByCreatedAtAsc(Pageable pageable);

    @Modifying
    @Query("UPDATE OutboxEventEntity e SET e.processed = true, e.processedAt = :now WHERE e.id = :id")
    void markProcessed(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Modifying
    @Query("""
            UPDATE OutboxEventEntity e
            SET e.retryCount = e.retryCount + 1, e.lastError = :error
            WHERE e.id = :id
            """)
    void markFailed(@Param("id") Long id, @Param("error") String error);
}
