package com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper;

import com.capysoft.tuevento.modules.language.domain.model.TranslationJob;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.TranslationJobEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Mapper para TranslationJob. El texto original se guarda como JSON.
 */
@Component
public class TranslationJobMapper {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<Map<String, String>> TEXTS = new TypeReference<>() { };

    static String toJson(Map<String, String> texts) {
        if (texts == null) {
            return null;
        }
        try {
            return JSON.writeValueAsString(texts);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Source texts cannot be serialized", e);
        }
    }

    static Map<String, String> fromJson(String json) {
        if (json == null) {
            return null;
        }
        try {
            return JSON.readValue(json, TEXTS);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored source texts are not valid JSON", e);
        }
    }

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
                .sourceTexts(fromJson(entity.getSourceTexts()))
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
                .sourceTexts(toJson(domain.getSourceTexts()))
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