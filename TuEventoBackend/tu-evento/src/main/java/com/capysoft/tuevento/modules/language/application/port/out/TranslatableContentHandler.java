package com.capysoft.tuevento.modules.language.application.port.out;

import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;

import java.util.Map;
import java.util.Optional;

/**
 * Port para desacoplar el módulo language de otros módulos.
 * Cada módulo dueño de contenido traducible implementa este port.
 */
public interface TranslatableContentHandler {

    /**
     * Tipo de entidad que maneja este handler.
     */
    String entityType();

    /**
     * Carga los textos fuente de una entidad.
     * 
     * @param entityId ID de la entidad
     * @return Mapa con clave=campo y valor=texto fuente
     */
    Map<String, String> loadSourceTexts(Long entityId);

    /**
     * Guarda la traducción de una entidad.
     * 
     * @param entityId ID de la entidad
     * @param languageCode Código del idioma destino
     * @param translatedTexts Mapa con clave=campo y valor=texto traducido
     * @param source Fuente de la traducción
     * @param status Estado de la traducción
     */
    void saveTranslation(Long entityId, String languageCode, Map<String, String> translatedTexts, 
                        TranslationSource source, TranslationStatus status);

    /**
     * Busca el estado actual de una traducción.
     * 
     * @param entityId ID de la entidad
     * @param languageCode Código del idioma
     * @return Estado actual si existe
     */
    Optional<TranslationStatus> findTranslationStatus(Long entityId, String languageCode);
}