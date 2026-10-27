package com.capysoft.tuevento.shared.application.translation;

import com.capysoft.tuevento.shared.domain.valueobject.TranslationJobStatus;

import java.util.Optional;

/**
 * Lo que los dominios necesitan de las traducciones automáticas: pedirlas, saber si se están
 * generando y descartarlas cuando cambia el texto original. Es la única puerta al núcleo de
 * traducción, para que pueda vivir en otro servicio sin tocar los dominios.
 *
 * <p>{@code entityType} es el tipo que atiende el {@code TranslatableContentHandler} del dominio
 * ("event", "profile", "category"…). El idioma origen es siempre el español.
 */
public interface TranslationRequests {

    /** Idioma en el que se escribe el contenido original. */
    String SOURCE_LANGUAGE = "es";

    /** Pide traducir la entidad a todos los idiomas activos. */
    void request(String entityType, long entityId);

    /** Estado del trabajo de traducción de la entidad a un idioma, si existe. */
    Optional<TranslationJobStatus> jobStatus(String entityType, long entityId, int languageId);

    /**
     * Para cuando se lee la entidad en un idioma y aún no hay traducción: la pide si nunca se
     * pidió e indica si se está generando (recién pedida, pendiente o en curso). Un trabajo
     * fallido no se vuelve a pedir aquí; lo reintenta el núcleo.
     *
     * @return {@code true} si el cliente debe esperar la traducción
     */
    default boolean ensureRequested(String entityType, long entityId, int languageId) {
        Optional<TranslationJobStatus> status = jobStatus(entityType, entityId, languageId);
        if (status.isEmpty()) {
            request(entityType, entityId);
            return true;
        }
        return status.get() == TranslationJobStatus.PENDING || status.get() == TranslationJobStatus.PROCESSING;
    }

    /**
     * Descarta los trabajos de traducción de la entidad (pendientes o no), p. ej. porque cambió
     * su texto original.
     *
     * @return cuántos trabajos se descartaron, o 0 si el descarte se hace en otro servicio
     */
    int discard(String entityType, long entityId);
}
