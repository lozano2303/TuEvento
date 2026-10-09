package com.capysoft.tuevento.modules.language.domain.model;

import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa una traducción de evento en el dominio.
 * Almacena las versiones traducidas de los campos de un evento.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventTranslation {

    private Integer translationId;
    private Long eventId;
    private Integer languageId;
    private String translatedName;
    private String translatedDescription;
    private TranslationSource source;
    private TranslationStatus status;

    /**
     * Marca la traducción como publicada.
     */
    public void publish() {
        this.status = TranslationStatus.PUBLISHED;
    }

    /**
     * Marca la traducción como pendiente de revisión.
     */
    public void markForReview() {
        this.status = TranslationStatus.PENDING_REVIEW;
    }

    /**
     * Verifica si la traducción está publicada.
     */
    public boolean isPublished() {
        return TranslationStatus.PUBLISHED == this.status;
    }

    /**
     * Verifica si todos los campos traducibles están completos.
     */
    public boolean isComplete() {
        return translatedName != null && !translatedName.trim().isEmpty() &&
               translatedDescription != null && !translatedDescription.trim().isEmpty();
    }
}