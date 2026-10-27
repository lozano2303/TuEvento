package com.capysoft.tuevento.modules.profile.domain.model;

import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Traducción de la biografía de un perfil a un idioma. Dos traducciones son iguales si son del
 * mismo perfil y el mismo idioma.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProfileTranslation {

    private Integer translationId;
    @EqualsAndHashCode.Include
    private Integer profileId;
    @EqualsAndHashCode.Include
    private Integer languageId;
    private String translatedBio;
    private TranslationSource source;
    private TranslationStatus status;
}
