package com.capysoft.tuevento.modules.catalogbridge.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.capysoft.tuevento.modules.catalogbridge.application.OrderSeatsSagaService;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.infrastructure.messaging.ProcessedEventStore;
import com.capysoft.tuevento.shared.infrastructure.outbox.OutboxHeaders;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("OrderEventsKafkaListener")
class OrderEventsKafkaListenerTest {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final String PAID = """
            {"orderId":1,"userId":5,"eventId":3,"seatIds":[10],"providerPaymentId":"tx"}""";

    private final OrderSeatsSagaService saga = mock(OrderSeatsSagaService.class);
    private final ProcessedEventStore processed = mock(ProcessedEventStore.class);
    private final OrderEventsKafkaListener listener = new OrderEventsKafkaListener(saga, processed, new ObjectMapper());

    private static ConsumerRecord<String, String> record(String type, String json) {
        ConsumerRecord<String, String> rec = new ConsumerRecord<>("tuevento.order", 0, 0L, "1", json);
        rec.headers().add(OutboxHeaders.EVENT_TYPE, type.getBytes(StandardCharsets.UTF_8));
        rec.headers().add(OutboxHeaders.EVENT_ID, EVENT_ID.toString().getBytes(StandardCharsets.UTF_8));
        return rec;
    }

    @Test
    @DisplayName("OrderPaid vende las sillas")
    void paidSells() {
        listener.onMessage(record("OrderPaid", PAID));

        verify(saga).sell(EVENT_ID, 1L, 5L, List.of(10), "Payment confirmed: tx");
    }

    @Test
    @DisplayName("si la silla ya no es del comprador, rechaza la venta")
    void businessFailureRejects() {
        BusinessException taken = new BusinessException("SEAT_ALREADY_TAKEN", "taken");
        doThrow(taken).when(saga).sell(any(), any(), any(), any(), any());

        listener.onMessage(record("OrderPaid", PAID));

        verify(saga).rejectSale(EVENT_ID, 1L, List.of(10), taken);
    }

    @Test
    @DisplayName("un error técnico se propaga para reintentar; no rechaza la venta")
    void technicalFailureRetries() {
        doThrow(new IllegalStateException("db down")).when(saga).sell(any(), any(), any(), any(), any());

        assertThatThrownBy(() -> listener.onMessage(record("OrderPaid", PAID))).isInstanceOf(IllegalStateException.class);
        verify(saga, never()).rejectSale(any(), any(), any(), any());
    }

    @Test
    @DisplayName("OrderReleased libera con el detalle, o con el motivo si no hay detalle")
    void releasedReleases() {
        listener.onMessage(record("OrderReleased", """
                {"orderId":1,"userId":5,"seatIds":[10],"reason":"CANCELLED","detail":"Order cancelled","changedBy":5}"""));
        verify(saga).release(EVENT_ID, 1L, 5L, List.of(10), 5, "Order cancelled");

        listener.onMessage(record("OrderReleased", """
                {"orderId":2,"userId":5,"seatIds":[10],"reason":"FAILED"}"""));
        verify(saga).release(EVENT_ID, 2L, 5L, List.of(10), null, "FAILED");
    }

    @Test
    @DisplayName("descarta otros tipos y mensajes incompletos")
    void discards() {
        listener.onMessage(record("OrderCreated", PAID));
        listener.onMessage(record("OrderPaid", "{\"orderId\":1}"));

        verifyNoInteractions(saga);
    }
}
