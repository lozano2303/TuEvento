package com.capysoft.geolocation.language.application.port.out;

import java.util.Map;

/**
 * Hacia dónde entrega el núcleo cada traducción terminada: al dominio dueño de la entidad.
 */
public interface TranslationResults {

    /**
     * @param translatedTexts campo → texto traducido, con las mismas claves que el texto original
     */
    void deliver(String entityType, long entityId, String languageCode, Map<String, String> translatedTexts);
}
