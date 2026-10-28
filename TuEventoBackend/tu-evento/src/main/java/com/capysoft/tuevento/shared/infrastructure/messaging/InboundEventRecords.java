package com.capysoft.tuevento.shared.infrastructure.messaging;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

import com.capysoft.tuevento.shared.infrastructure.outbox.OutboxHeaders;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Reads a domain event received over Kafka from the outbox, so every consumer filters by type,
 * deduplicates by {@code eventId} and discards unreadable messages the same way.
 */
@Slf4j
public final class InboundEventRecords {

    /**
     * An event still to be handled: its type, its {@code eventId} ({@code null} when the header is
     * missing) and its payload.
     */
    public record Inbound<T>(String eventType, UUID eventId, T payload) {
    }

    private InboundEventRecords() {
    }

    /** Same as {@link #readNew(ConsumerRecord, Set, String, ProcessedEventStore, ObjectMapper, Class)} for one type. */
    public static <T> Optional<Inbound<T>> readNew(ConsumerRecord<String, String> rec, String eventType,
                                                   String consumer, ProcessedEventStore processedEventStore,
                                                   ObjectMapper objectMapper, Class<T> type) {
        return readNew(rec, Set.of(eventType), consumer, processedEventStore, objectMapper, type);
    }

    /**
     * The event, when the record is of one of {@code eventTypes}, {@code consumer} has not processed it yet and
     * its payload can be read as {@code type}. An unreadable message can never succeed on retry, so it is
     * logged and discarded like the others.
     */
    public static <T> Optional<Inbound<T>> readNew(ConsumerRecord<String, String> rec, Set<String> eventTypes,
                                                   String consumer, ProcessedEventStore processedEventStore,
                                                   ObjectMapper objectMapper, Class<T> type) {
        String eventType = header(rec, OutboxHeaders.EVENT_TYPE);
        if (eventType == null || !eventTypes.contains(eventType)) {
            return Optional.empty();
        }
        UUID eventId = eventId(rec);
        if (eventId != null && processedEventStore.isProcessed(consumer, eventId)) {
            log.debug("{} {} already processed by {}, skipping", eventType, eventId, consumer);
            return Optional.empty();
        }
        try {
            return Optional.of(new Inbound<>(eventType, eventId, objectMapper.readValue(rec.value(), type)));
        } catch (Exception e) {
            log.error("Discarding unreadable {}: offset={}, eventId={}: {}",
                    eventType, rec.offset(), eventId, e.getMessage());
            return Optional.empty();
        }
    }

    private static UUID eventId(ConsumerRecord<String, String> rec) {
        String value = header(rec, OutboxHeaders.EVENT_ID);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String header(ConsumerRecord<String, String> rec, String name) {
        Header header = rec.headers().lastHeader(name);
        return header == null || header.value() == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
