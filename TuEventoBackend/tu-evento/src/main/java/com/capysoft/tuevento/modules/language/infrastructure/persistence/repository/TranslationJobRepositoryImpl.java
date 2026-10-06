package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.domain.model.TranslationJob;
import com.capysoft.tuevento.modules.language.domain.repository.TranslationJobRepository;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.TranslationJobEntity;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.repository.LanguageJpaRepository;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.repository.TranslationJobJpaRepository;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper.TranslationJobMapper;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationJobStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementación del repositorio de TranslationJob.
 */
@Repository
@RequiredArgsConstructor
public class TranslationJobRepositoryImpl implements TranslationJobRepository {

    private final TranslationJobJpaRepository jpaRepository;
    private final LanguageJpaRepository languageJpaRepository;
    private final TranslationJobMapper mapper;

    @Override
    public Optional<TranslationJob> findById(Long jobId) {
        return jpaRepository.findById(jobId)
                .map(mapper::toDomain);
    }

    @Override
    public List<TranslationJob> findByEntityAndStatus(String entityType, Long entityId, TranslationJobStatus status) {
        return jpaRepository.findByEntityAndStatus(entityType, entityId, status.name().toLowerCase())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TranslationJob> findJobsForRetry(LocalDateTime now, int maxAttempts) {
        return jpaRepository.findJobsForRetry(now, maxAttempts)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TranslationJob> findProcessingJobsWithTimeout(LocalDateTime timeoutThreshold) {
        return jpaRepository.findProcessingJobsWithTimeout(timeoutThreshold)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean claimJob(Long jobId) {
        return jpaRepository.claimJob(jobId, 
                                     TranslationJobStatus.PROCESSING.name().toLowerCase(),
                                     TranslationJobStatus.PENDING.name().toLowerCase(), 
                                     TranslationJobStatus.FAILED.name().toLowerCase()) > 0;
    }

    @Override
    public Optional<TranslationJob> findByEntityAndTargetLanguage(String entityType, Long entityId, Long targetLanguageId) {
        return jpaRepository.findByEntityAndTargetLanguage(entityType, entityId, targetLanguageId)
                .stream()
                .findFirst()
                .map(mapper::toDomain);
    }

    @Override
    public TranslationJob save(TranslationJob job) {
        TranslationJobEntity entity = mapper.toEntity(job);
        
        // Cargar las entidades de idioma si no están presentes
        if (entity.getSourceLanguage() == null && job.getSourceLanguageId() != null) {
            entity.setSourceLanguage(languageJpaRepository.getReferenceById(job.getSourceLanguageId()));
        }
        if (entity.getTargetLanguage() == null && job.getTargetLanguageId() != null) {
            entity.setTargetLanguage(languageJpaRepository.getReferenceById(job.getTargetLanguageId()));
        }
        
        TranslationJobEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public List<TranslationJob> saveAll(List<TranslationJob> jobs) {
        List<TranslationJobEntity> entities = jobs.stream()
                .map(job -> {
                    TranslationJobEntity entity = mapper.toEntity(job);
                    // Cargar las entidades de idioma
                    if (entity.getSourceLanguage() == null && job.getSourceLanguageId() != null) {
                        entity.setSourceLanguage(languageJpaRepository.getReferenceById(job.getSourceLanguageId()));
                    }
                    if (entity.getTargetLanguage() == null && job.getTargetLanguageId() != null) {
                        entity.setTargetLanguage(languageJpaRepository.getReferenceById(job.getTargetLanguageId()));
                    }
                    return entity;
                })
                .collect(Collectors.toList());
                
        return jpaRepository.saveAll(entities)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}