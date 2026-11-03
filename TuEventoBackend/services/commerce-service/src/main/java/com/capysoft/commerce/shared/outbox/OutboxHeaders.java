package com.capysoft.commerce.shared.outbox;

/**
 * Headers que el {@link OutboxRelayScheduler} añade a cada mensaje de Kafka. El payload no
 * cambia: los consumidores que solo leen el JSON siguen funcionando igual.
 *
 * <p>Cada agregado publica en su topic {@code tuevento.<aggregateType>} con key {@code aggregateId};
 * el tipo de evento va en {@link #EVENT_TYPE}. Los consumidores deben descartar duplicados por
 * {@link #EVENT_ID}: la entrega es at-least-once y un reenvío conserva el mismo valor.
 */
public final class OutboxHeaders {

    /** UUID estable del evento de negocio; igual en todos los reintentos. */
    public static final String EVENT_ID = "eventId";
    public static final String EVENT_TYPE = "eventType";
    public static final String AGGREGATE_TYPE = "aggregateType";
    public static final String AGGREGATE_ID = "aggregateId";
    /** Momento en que se guardó el evento en el outbox, como instante ISO-8601 en UTC. */
    public static final String OCCURRED_AT = "occurredAt";
    /** Versión de este contrato de headers; subirla si cambia el significado de alguno. */
    public static final String SCHEMA_VERSION = "schemaVersion";
    public static final String CURRENT_SCHEMA_VERSION = "1";

    private OutboxHeaders() {
    }
}
