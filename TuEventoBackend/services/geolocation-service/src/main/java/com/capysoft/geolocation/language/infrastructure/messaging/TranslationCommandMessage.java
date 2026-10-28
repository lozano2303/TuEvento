package com.capysoft.geolocation.language.infrastructure.messaging;

import java.util.Map;

/**
 * Orden del monolito sobre las traducciones de una entidad.
 *
 * @param action         {@link #REQUEST} (traducir {@code texts} a los idiomas activos) o
 *                       {@link #DISCARD} (borrar sus trabajos, p. ej. porque cambió el texto)
 * @param sourceLanguage código del idioma de {@code texts}; solo en REQUEST
 * @param texts          campo → texto original; solo en REQUEST
 */
public record TranslationCommandMessage(String action, String entityType, Long entityId,
                                        String sourceLanguage, Map<String, String> texts) {

    public static final String REQUEST = "REQUEST";
    public static final String DISCARD = "DISCARD";
}
