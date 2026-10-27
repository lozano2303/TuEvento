package com.capysoft.geolocation.language.domain.repository;

import com.capysoft.geolocation.language.domain.model.TranslationJob;
import com.capysoft.geolocation.language.domain.model.TranslationJobStatus;

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
    Optional<TranslationJob> findById(Integer jobId);

    /**
     * Busca jobs por entidad y estado.
     */
    List<TranslationJob> findByEntityAndStatus(String entityType, Integer entityId, TranslationJobStatus status);

    /**
     * Busca todos los jobs con un estado específico.
     */
    List<TranslationJob> findByStatus(TranslationJobStatus status);

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
    boolean claimJob(Integer jobId);

    /**
     * Busca job existente por entidad y idioma destino.
     */
    Optional<TranslationJob> findByEntityAndTargetLanguage(String entityType, Integer entityId, Integer targetLanguageId);

    /**
     * Guarda un job.
     */
    TranslationJob save(TranslationJob job);

    /**
     * Guarda múltiples jobs.
     */
    List<TranslationJob> saveAll(List<TranslationJob> jobs);

    /**
     * Elimina todos los jobs de una entidad específica.
     * 
     * @param entityType Tipo de entidad (ej: "profile", "event", "category")
     * @param entityId ID de la entidad
     * @return Cantidad de jobs eliminados
     */
    int deleteByEntity(String entityType, Integer entityId);
}