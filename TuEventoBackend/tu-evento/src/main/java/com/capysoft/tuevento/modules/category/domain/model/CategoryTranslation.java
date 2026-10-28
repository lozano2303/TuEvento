package com.capysoft.tuevento.modules.category.domain.model;

import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa una traducción de categoría en el dominio.
 * Almacena las versiones traducidas de los campos de una categoría.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTranslation {

    private Integer translationId;
    private Integer categoryId;
    private Integer languageId;
    private String translatedName;
    private String translatedDescription;
    private TranslationSource source;
    private TranslationStatus status;
}
