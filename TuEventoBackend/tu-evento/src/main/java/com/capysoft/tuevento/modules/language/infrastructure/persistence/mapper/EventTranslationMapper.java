package com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper;

import com.capysoft.tuevento.modules.language.domain.model.EventTranslation;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.EventTranslationEntity;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.LanguageEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper entre EventTranslation (dominio) y EventTranslationEntity (persistencia).
 */
@Component
public class EventTranslationMapper {

    /**
     * Convierte de entidad JPA a modelo de dominio.
     */
    public EventTranslation toDomain(EventTranslationEntity entity) {
        if (entity == null) {
            return null;
        }

        return EventTranslation.builder()
                .translationId(entity.getTranslationId())
                .eventId(entity.getEventId())
                .languageId(entity.getLanguageId())
                .translatedName(entity.getTranslatedName())
                .translatedDescription(entity.getTranslatedDescription())
                .source(entity.getSource())
                .status(entity.getStatus())
                .build();
    }

    /**
     * Convierte de modelo de dominio a entidad JPA.
     */
    public EventTranslationEntity toEntity(EventTranslation domain) {
        if (domain == null) {
            return null;
        }

        return EventTranslationEntity.builder()
                .translationId(domain.getTranslationId())
                .eventId(domain.getEventId())
                .languageId(domain.getLanguageId())
                .translatedName(domain.getTranslatedName())
                .translatedDescription(domain.getTranslatedDescription())
                .source(domain.getSource())
                .status(domain.getStatus())
                .build();
    }

    /**
     * Actualiza una entidad existente con datos del dominio.
     */
    public void updateEntity(EventTranslationEntity entity, EventTranslation domain) {
        if (entity == null || domain == null) {
            return;
        }

        entity.setTranslatedName(domain.getTranslatedName());
        entity.setTranslatedDescription(domain.getTranslatedDescription());
        entity.setSource(domain.getSource());
        entity.setStatus(domain.getStatus());
    }
}