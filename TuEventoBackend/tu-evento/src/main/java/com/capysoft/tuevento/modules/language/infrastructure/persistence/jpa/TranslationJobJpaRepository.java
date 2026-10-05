package com.capysoft.tuevento.modules.language.infrastructure.persistence.jpa;

import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.TranslationJobEntity;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para TranslationJobEntity.
 */
public interface TranslationJobJpaRepository extends JpaRepository<TranslationJobEntity, Long> {

    /**
     * Busca jobs por entidad y estado.
     */
    @Query("SELECT t FROM TranslationJobEntity t WHERE t.entityType = :entityType AND t.entityId = :entityId AND t.status = :status")
    List<TranslationJobEntity> findByEntityAndStatus(@Param("entityType") String entityType, 
                                                      @Param("entityId") Long entityId, 
                                                      @Param("status") TranslationJobStatus status);

    /**
     * Busca jobs para reintento.
     */
    @Query("SELECT t FROM TranslationJobEntity t WHERE t.status IN ('FAILED', 'PENDING') AND " +
           "(t.nextRetryAt IS NULL OR t.nextRetryAt <= :now) AND t.attempts < :maxAttempts")
    List<TranslationJobEntity> findJobsForRetry(@Param("now") LocalDateTime now, 
                                                 @Param("maxAttempts") int maxAttempts);

    /**
     * Busca jobs en procesamiento con timeout.
     */
    @Query("SELECT t FROM TranslationJobEntity t WHERE t.status = 'PROCESSING' AND t.updatedAt < :timeoutThreshold")
    List<TranslationJobEntity> findProcessingJobsWithTimeout(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);

    /**
     * Reclama un job atómicamente.
     */
    @Modifying
    @Transactional
    @Query("UPDATE TranslationJobEntity t SET t.status = :processingStatus, t.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE t.jobId = :jobId AND t.status IN (:pendingStatus, :failedStatus)")
    int claimJob(@Param("jobId") Long jobId, 
                 @Param("processingStatus") TranslationJobStatus processingStatus,
                 @Param("pendingStatus") TranslationJobStatus pendingStatus, 
                 @Param("failedStatus") TranslationJobStatus failedStatus);

    /**
     * Busca job existente por entidad y idioma destino.
     */
    @Query("SELECT t FROM TranslationJobEntity t WHERE t.entityType = :entityType AND t.entityId = :entityId AND t.targetLanguage.languageId = :targetLanguageId")
    Optional<TranslationJobEntity> findByEntityAndTargetLanguage(@Param("entityType") String entityType,
                                                                  @Param("entityId") Long entityId,
                                                                  @Param("targetLanguageId") Long targetLanguageId);
}