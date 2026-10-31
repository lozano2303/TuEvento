package com.capysoft.commerce.payment.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.capysoft.commerce.payment.application.port.in.RefundRejectedSaleUseCase;
import com.capysoft.commerce.shared.messaging.ProcessedEventStore;
import com.capysoft.commerce.shared.outbox.OutboxHeaders;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("SeatsSaleRejectedKafkaListener")
class SeatsSaleRejectedKafkaListenerTest {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final String MESSAGE = "{\"orderId\":9,\"seatIds\":[1],\"reason\":\"SEAT_ALREADY_TAKEN\"}";

    private final RefundRejectedSaleUseCase refunds = mock(RefundRejectedSaleUseCase.class);
    private final ProcessedEventStore processed = mock(ProcessedEventStore.class);
    private final SeatsSaleRejectedKafkaListener listener =
            new SeatsSaleRejectedKafkaListener(refunds, processed, new ObjectMapper());

    private static ConsumerRecord<String, String> record(String type, String json) {
        ConsumerRecord<String, String> rec = new ConsumerRecord<>("tuevento.seat", 0, 0L, "9", json);
        rec.headers().add(OutboxHeaders.EVENT_TYPE, type.getBytes(StandardCharsets.UTF_8));
        rec.headers().add(OutboxHeaders.EVENT_ID, EVENT_ID.toString().getBytes(StandardCharsets.UTF_8));
        return rec;
    }

    @Test
    @DisplayName("reembolsa la orden y marca el mensaje")
    void refunds() {
        listener.onMessage(record("SeatsSaleRejected", MESSAGE));

        verify(refunds).execute(9L, "SEAT_ALREADY_TAKEN");
        verify(processed).markProcessed(SeatsSaleRejectedKafkaListener.CONSUMER, EVENT_ID);
    }

    @Test
    @DisplayName("un mensaje ya procesado, de otro tipo o sin orden se descarta")
    void discards() {
        listener.onMessage(record("OtherEvent", MESSAGE));
        listener.onMessage(record("SeatsSaleRejected", "{}"));
        when(processed.isProcessed(SeatsSaleRejectedKafkaListener.CONSUMER, EVENT_ID)).thenReturn(true);
        listener.onMessage(record("SeatsSaleRejected", MESSAGE));

        verifyNoInteractions(refunds);
    }

    @Test
    @DisplayName("si el reembolso falla se propaga y no se marca: el contenedor reintenta")
    void failureRetries() {
        doThrow(new IllegalStateException("db down")).when(refunds).execute(any(), any());

        assertThatThrownBy(() -> listener.onMessage(record("SeatsSaleRejected", MESSAGE)))
                .isInstanceOf(IllegalStateException.class);
        verify(processed, never()).markProcessed(any(), any());
    }
}
