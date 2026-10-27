package com.capysoft.tuevento.modules.section.domain.model;

import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Traducción del nombre de un tipo de sección a un idioma. Dos traducciones son iguales si
 * son del mismo tipo de sección y el mismo idioma.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SectionTypeTranslation {

    private Integer translationId;
    @EqualsAndHashCode.Include
    private Integer sectionTypeId;
    @EqualsAndHashCode.Include
    private Integer languageId;
    private String translatedName;
    private TranslationSource source;
    private TranslationStatus status;
}
