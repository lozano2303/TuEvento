package com.capysoft.tuevento.shared.infrastructure.translation;

/**
 * Topics entre el monolito y el núcleo de traducción del servicio de localización (modo
 * {@code remote}). La clave de cada mensaje es {@code entityType:entityId}: las órdenes de una
 * misma entidad se procesan en el orden en que se publicaron.
 */
public final class TranslationTopics {

    /** Monolito → servicio: pedir la traducción de una entidad (con su texto) o descartar sus trabajos. */
    public static final String COMMANDS = "tuevento.localizacion.translation.command";
    /** Servicio → monolito: una traducción terminada, para guardarla en el dominio dueño. */
    public static final String RESULTS = "tuevento.localizacion.translation.result";

    private TranslationTopics() {
    }

    static String key(String entityType, long entityId) {
        return entityType + ":" + entityId;
    }
}
