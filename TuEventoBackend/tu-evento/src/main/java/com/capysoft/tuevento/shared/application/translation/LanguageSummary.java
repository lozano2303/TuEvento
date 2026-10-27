package com.capysoft.tuevento.shared.application.translation;

import lombok.Builder;
import lombok.Value;

/** Lo que un dominio necesita de un idioma: su id y su código. */
@Value
@Builder
public class LanguageSummary {
    Integer languageId;
    String code;
}
