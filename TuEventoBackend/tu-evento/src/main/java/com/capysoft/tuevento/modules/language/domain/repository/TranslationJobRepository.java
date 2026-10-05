package com.capysoft.tuevento.modules.language.domain.repository;

import com.capysoft.tuevento.modules.language.domain.model.TranslationJob;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationJobStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de dominio para TranslationJob.
 */
public interface TranslationJobRepository {

    /**
     * Busca un job por su ID.
     */
    Optional<TranslationJob> findById(Long jobId);

    /**
     * Busca jobs por entidad y estado.
     */
    List<TranslationJob> findByEntityAndStatus(String entityType, Long entityId, TranslationJobStatus status);

    /**
     * Busca jobs para reintento.
     */
    List<TranslationJob> findJobsForRetry(LocalDateTime now, int maxAttempts);

    /**
     * Busca jobs en procesamiento con timeout.
     */
    List<TranslationJob> findProcessingJobsWithTimeout(LocalDateTime timeoutThreshold);

    /**
     * Reclama un job atómicamente (PENDING/FAILED -> PROCESSING).
     * 
     * @param jobId ID del job
     * @return true si se pudo reclamar, false si ya está siendo procesado
     */
    boolean claimJob(Long jobId);

    /**
     * Busca job existente por entidad y idioma destino.
     */
    Optional<TranslationJob> findByEntityAndTargetLanguage(String entityType, Long entityId, Long targetLanguageId);

    /**
     * Guarda un job.
     */
    TranslationJob save(TranslationJob job);

    /**
     * Guarda múltiples jobs.
     */
    List<TranslationJob> saveAll(List<TranslationJob> jobs);
}