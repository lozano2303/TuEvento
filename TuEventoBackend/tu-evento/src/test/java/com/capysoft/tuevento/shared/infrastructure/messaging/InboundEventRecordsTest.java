package com.capysoft.tuevento.shared.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.capysoft.tuevento.shared.infrastructure.messaging.InboundEventRecords.Inbound;
import com.capysoft.tuevento.shared.infrastructure.outbox.OutboxHeaders;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("InboundEventRecords")
class InboundEventRecordsTest {

    private static final String TYPE = "UserActivated";
    private static final String CONSUMER = "test.consumer";

    record Payload(Integer userId) {
    }

    private final ProcessedEventStore processed = mock(ProcessedEventStore.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static ConsumerRecord<String, String> rec(String type, String eventId, String payload) {
        ConsumerRecord<String, String> rec = new ConsumerRecord<>("tuevento.user", 0, 3L, "7", payload);
        if (type != null) {
            rec.headers().add(OutboxHeaders.EVENT_TYPE, type.getBytes(StandardCharsets.UTF_8));
        }
        if (eventId != null) {
            rec.headers().add(OutboxHeaders.EVENT_ID, eventId.getBytes(StandardCharsets.UTF_8));
        }
        return rec;
    }

    private java.util.Optional<Inbound<Payload>> read(ConsumerRecord<String, String> rec) {
        return InboundEventRecords.readNew(rec, TYPE, CONSUMER, processed, objectMapper, Payload.class);
    }

    @Test
    @DisplayName("returns the eventId and payload of a new event of the expected type")
    void readsNewEvent() {
        UUID id = UUID.randomUUID();

        assertThat(read(rec(TYPE, id.toString(), "{\"userId\":7}")))
                .contains(new Inbound<>(id, new Payload(7)));
    }

    @Test
    @DisplayName("skips other event types and records without a type, without touching the store")
    void skipsOtherTypes() {
        assertThat(read(rec("OrganizerRoleRevoked", UUID.randomUUID().toString(), "{}"))).isEmpty();
        assertThat(read(rec(null, UUID.randomUUID().toString(), "{}"))).isEmpty();
        verifyNoInteractions(processed);
    }

    @Test
    @DisplayName("skips an event the consumer already processed")
    void skipsProcessed() {
        UUID id = UUID.randomUUID();
        when(processed.isProcessed(CONSUMER, id)).thenReturn(true);

        assertThat(read(rec(TYPE, id.toString(), "{\"userId\":7}"))).isEmpty();
    }

    @Test
    @DisplayName("a missing or malformed eventId still delivers the event, just without dedupe")
    void missingOrMalformedEventId() {
        assertThat(read(rec(TYPE, null, "{\"userId\":7}"))).contains(new Inbound<>(null, new Payload(7)));
        assertThat(read(rec(TYPE, "not-a-uuid", "{\"userId\":7}"))).contains(new Inbound<>(null, new Payload(7)));
        verifyNoInteractions(processed);
    }

    @Test
    @DisplayName("an unreadable payload is discarded")
    void discardsUnreadablePayload() {
        assertThat(read(rec(TYPE, UUID.randomUUID().toString(), "{not json"))).isEmpty();
    }
}
