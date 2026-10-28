package com.capysoft.geolocation.language.infrastructure.messaging;

import java.util.Map;

/**
 * Traducción terminada de una entidad a un idioma.
 *
 * @param texts campo → texto traducido, con las mismas claves que el texto original
 */
public record TranslationResultMessage(String entityType, Long entityId, String languageCode,
                                       Map<String, String> texts) {
}
