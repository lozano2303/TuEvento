package com.capysoft.geolocation.language.infrastructure.messaging;

/**
 * Topics entre el monolito y el núcleo de traducción. La clave de cada mensaje es
 * {@code entityType:entityId}: las órdenes de una misma entidad caen en la misma partición y se
 * procesan en el orden en que se publicaron (un descarte nunca adelanta a una petición previa).
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
