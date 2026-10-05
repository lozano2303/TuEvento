package com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper;

import com.capysoft.tuevento.modules.language.domain.model.TranslationJob;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.TranslationJobEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper para TranslationJob.
 */
@Component
public class TranslationJobMapper {

    /**
     * Convierte de entidad a dominio.
     */
    public TranslationJob toDomain(TranslationJobEntity entity) {
        if (entity == null) {
            return null;
        }

        return TranslationJob.builder()
                .jobId(entity.getJobId())
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .sourceLanguageId(entity.getSourceLanguage() != null ? entity.getSourceLanguage().getLanguageId() : null)
                .targetLanguageId(entity.getTargetLanguage() != null ? entity.getTargetLanguage().getLanguageId() : null)
                .sourceHash(entity.getSourceHash())
                .status(entity.getStatus())
                .provider(entity.getProvider())
                .attempts(entity.getAttempts())
                .lastError(entity.getLastError())
                .nextRetryAt(entity.getNextRetryAt())
                .completedAt(entity.getCompletedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    /**
     * Convierte de dominio a entidad.
     */
    public TranslationJobEntity toEntity(TranslationJob domain) {
        if (domain == null) {
            return null;
        }

        TranslationJobEntity entity = TranslationJobEntity.builder()
                .jobId(domain.getJobId())
                .entityType(domain.getEntityType())
                .entityId(domain.getEntityId())
                .sourceHash(domain.getSourceHash())
                .status(domain.getStatus())
                .provider(domain.getProvider())
                .attempts(domain.getAttempts())
                .lastError(domain.getLastError())
                .nextRetryAt(domain.getNextRetryAt())
                .completedAt(domain.getCompletedAt())
                .build();

        // Las referencias de idioma se establecen en el repositorio
        return entity;
    }
}