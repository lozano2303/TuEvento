package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.TranslationJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TranslationJobJpaRepository extends JpaRepository<TranslationJobEntity, Long> {

    @Query("SELECT tj FROM TranslationJobEntity tj WHERE tj.status = :status")
    List<TranslationJobEntity> findByStatus(@Param("status") String status);

    @Query("SELECT tj FROM TranslationJobEntity tj WHERE tj.sourceLanguage.languageId = :sourceLanguageId")
    List<TranslationJobEntity> findBySourceLanguageId(@Param("sourceLanguageId") Long sourceLanguageId);

    @Query("SELECT tj FROM TranslationJobEntity tj WHERE tj.targetLanguage.languageId = :targetLanguageId")
    List<TranslationJobEntity> findByTargetLanguageId(@Param("targetLanguageId") Long targetLanguageId);

    @Query("SELECT tj FROM TranslationJobEntity tj WHERE tj.entityType = :entityType")
    List<TranslationJobEntity> findByEntityType(@Param("entityType") String entityType);

    @Query("SELECT tj FROM TranslationJobEntity tj WHERE tj.entityType = :entityType AND tj.entityId = :entityId")
    List<TranslationJobEntity> findByEntityTypeAndEntityId(@Param("entityType") String entityType, 
                                                           @Param("entityId") Long entityId);

    @Query("SELECT tj FROM TranslationJobEntity tj WHERE tj.createdAt BETWEEN :startDate AND :endDate")
    List<TranslationJobEntity> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                                      @Param("endDate") LocalDateTime endDate);

    @Query("SELECT tj FROM TranslationJobEntity tj WHERE tj.status = :status AND tj.targetLanguage.languageId = :targetLanguageId ORDER BY tj.createdAt ASC")
    List<TranslationJobEntity> findPendingJobsByTargetLanguage(@Param("status") String status, 
                                                               @Param("targetLanguageId") Long targetLanguageId);

    @Query("SELECT tj FROM TranslationJobEntity tj WHERE tj.entityType = :entityType AND tj.entityId = :entityId AND tj.status = :status")
    List<TranslationJobEntity> findByEntityAndStatus(@Param("entityType") String entityType, 
                                                     @Param("entityId") Long entityId, 
                                                     @Param("status") String status);

    @Query("SELECT tj FROM TranslationJobEntity tj WHERE tj.createdAt < :cutoffTime AND tj.retryCount < :maxRetryCount AND tj.status IN ('FAILED', 'TIMEOUT') ORDER BY tj.createdAt ASC")
    List<TranslationJobEntity> findJobsForRetry(@Param("cutoffTime") LocalDateTime cutoffTime, 
                                               @Param("maxRetryCount") int maxRetryCount);

    @Query("SELECT tj FROM TranslationJobEntity tj WHERE tj.status = 'PROCESSING' AND tj.updatedAt < :timeoutTime")
    List<TranslationJobEntity> findProcessingJobsWithTimeout(@Param("timeoutTime") LocalDateTime timeoutTime);

    @Modifying
    @Query("UPDATE TranslationJobEntity tj SET tj.status = :newStatus, tj.updatedAt = CURRENT_TIMESTAMP WHERE tj.jobId = :jobId AND tj.status IN (:currentStatus1, :currentStatus2)")
    int claimJob(@Param("jobId") Long jobId, 
                @Param("newStatus") String newStatus,
                @Param("currentStatus1") String currentStatus1, 
                @Param("currentStatus2") String currentStatus2);

    @Query("SELECT tj FROM TranslationJobEntity tj WHERE tj.entityType = :entityType AND tj.entityId = :entityId AND tj.targetLanguage.languageId = :targetLanguageId")
    List<TranslationJobEntity> findByEntityAndTargetLanguage(@Param("entityType") String entityType, 
                                                            @Param("entityId") Long entityId, 
                                                            @Param("targetLanguageId") Long targetLanguageId);
}