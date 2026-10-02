package com.capysoft.tuevento.shared.domain.valueobject;

/**
 * Estado de una traducción.
 */
public enum TranslationStatus {
    /** Traducción automática generada por IA/servicios */
    AUTOMATIC,
    
    /** Traducción revisada y aprobada por humanos */
    REVIEWED,
    
    /** Traducción rechazada/marcada como incorrecta */
    REJECTED
}