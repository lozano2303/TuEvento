package com.capysoft.tuevento.modules.event.domain.model;

import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Traducción del texto de un comentario (rating) de evento a un idioma. Dos traducciones son
 * iguales si son del mismo comentario y el mismo idioma.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EventRatingTranslation {

    private Integer translationId;
    @EqualsAndHashCode.Include
    private Integer ratingId;
    @EqualsAndHashCode.Include
    private Integer languageId;
    private String translatedComment;
    private TranslationStatus status;
}
