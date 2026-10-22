package com.capysoft.tuevento.shared.domain.valueobject;

/**
 * Estado de una traducción.
 * Indica el nivel de calidad y revisión de la traducción.
 * Basado en translation_status_enum del DBML.
 */
public enum TranslationStatus {
    /** Borrador inicial, no publicada */
    DRAFT,
    
    /** Traducción publicada y activa */
    PUBLISHED,
    
    /** Pendiente de revisión por moderador */
    PENDING_REVIEW
}