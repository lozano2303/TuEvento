package com.capysoft.tuevento.modules.language.application.port.out;

/**
 * Excepción lanzada cuando ocurre un error en la traducción.
 */
public class TranslationException extends Exception {

    public TranslationException(String message) {
        super(message);
    }

    public TranslationException(String message, Throwable cause) {
        super(message, cause);
    }
}