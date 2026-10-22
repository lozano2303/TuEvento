package com.capysoft.tuevento.shared.domain.valueobject;

/**
 * Estados posibles de un job de traducción.
 */
public enum TranslationJobStatus {
    /**
     * Job creado y esperando procesamiento.
     */
    PENDING,

    /**
     * Job siendo procesado actualmente.
     */
    PROCESSING,

    /**
     * Job completado exitosamente.
     */
    COMPLETED,

    /**
     * Job falló después de uno o más intentos.
     */
    FAILED
}