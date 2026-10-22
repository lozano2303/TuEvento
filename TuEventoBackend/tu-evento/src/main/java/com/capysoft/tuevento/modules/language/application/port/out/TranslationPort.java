package com.capysoft.tuevento.modules.language.application.port.out;

/**
 * Port para servicios de traducción externa.
 */
public interface TranslationPort {

    /**
     * Traduce un texto de un idioma a otro.
     * 
     * @param sourceText Texto fuente
     * @param sourceLang Código del idioma fuente
     * @param targetLang Código del idioma destino
     * @return Texto traducido
     * @throws TranslationException Si ocurre un error en la traducción
     */
    String translate(String sourceText, String sourceLang, String targetLang) throws TranslationException;

    /**
     * Verifica si el servicio de traducción está disponible.
     * 
     * @return true si está disponible, false en caso contrario
     */
    boolean isAvailable();
}