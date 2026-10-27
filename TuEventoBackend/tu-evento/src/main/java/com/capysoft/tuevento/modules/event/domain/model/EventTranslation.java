package com.capysoft.tuevento.modules.event.domain.model;

import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Traducción del nombre y la descripción de un evento a un idioma.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventTranslation {

    private Integer translationId;
    private Integer eventId;
    private Integer languageId;
    private String translatedName;
    private String translatedDescription;
    private TranslationSource source;
    private TranslationStatus status;
}
