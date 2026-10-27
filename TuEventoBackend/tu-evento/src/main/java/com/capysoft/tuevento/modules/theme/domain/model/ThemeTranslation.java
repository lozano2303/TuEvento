package com.capysoft.tuevento.modules.theme.domain.model;

import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Traducción del nombre y la descripción de un tema a un idioma. Se siembran por migración y
 * solo se muestran las publicadas.
 */
@Getter
@Setter
@NoArgsConstructor
public class ThemeTranslation {

    private Integer translationId;
    private Integer themeId;
    private Integer languageId;
    private String translatedName;
    private String translatedDescription;
    private TranslationStatus status;
}
