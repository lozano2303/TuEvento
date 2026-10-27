package com.capysoft.tuevento.shared.infrastructure.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.capysoft.tuevento.shared.domain.outbox.OutboxEvent;
import com.capysoft.tuevento.shared.domain.outbox.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxEventPublisher")
class OutboxEventPublisherTest {

    @Mock OutboxEventRepository outboxEventRepository;

    @Test
    @DisplayName("guarda el payload como JSON en el topic del agregado, con el tipo normalizado a minúsculas")
    void savesJsonPayloadWithDerivedTopic() {
        OutboxEventPublisher publisher = new OutboxEventPublisher(outboxEventRepository, new ObjectMapper());

        publisher.publish("Order", "9", "OrderPaid", Map.of("orderId", 9));

        ArgumentCaptor<OutboxEvent> saved = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(saved.capture());
        OutboxEvent event = saved.getValue();
        assertThat(event.getTopic()).isEqualTo("tuevento.order");
        assertThat(event.getAggregateType()).isEqualTo("order");
        assertThat(event.getEventType()).isEqualTo("OrderPaid");
        assertThat(event.getPayload()).isEqualTo("{\"orderId\":9}");
        assertThat(event.getAggregateId()).isEqualTo("9");
        assertThat(event.isProcessed()).isFalse();
        assertThat(event.getCreatedAt()).isNotNull();
        assertThat(event.getEventId()).isNotNull();
    }

    @Test
    @DisplayName("cada publicación es un evento de negocio distinto, con su propio eventId")
    void eachPublishGetsItsOwnEventId() {
        OutboxEventPublisher publisher = new OutboxEventPublisher(outboxEventRepository, new ObjectMapper());

        publisher.publish("order", "9", "OrderPaid", Map.of("orderId", 9));
        publisher.publish("order", "9", "OrderPaid", Map.of("orderId", 9));

        ArgumentCaptor<OutboxEvent> saved = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues().get(0).getEventId()).isNotEqualTo(saved.getAllValues().get(1).getEventId());
    }

    @Test
    @DisplayName("respeta el eventId que elige quien publica, sin cambiar el topic")
    void keepsCallerEventId() {
        OutboxEventPublisher publisher = new OutboxEventPublisher(outboxEventRepository, new ObjectMapper());
        UUID eventId = UUID.randomUUID();

        publisher.publish("order", "9", "OrderPaid", Map.of("orderId", 9), eventId);

        ArgumentCaptor<OutboxEvent> saved = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(saved.capture());
        assertThat(saved.getValue().getEventId()).isEqualTo(eventId);
        assertThat(saved.getValue().getTopic()).isEqualTo("tuevento.order");
    }

    @Test
    @DisplayName("todos los eventos de un agregado van al mismo topic, sea cual sea su tipo")
    void eventTypesOfAnAggregateShareTopic() {
        OutboxEventPublisher publisher = new OutboxEventPublisher(outboxEventRepository, new ObjectMapper());

        publisher.publish("order", "9", "SeatsHeld", Map.of("orderId", 9));
        publisher.publish(" Order ", "9", "OrderPaid", Map.of("orderId", 9));

        ArgumentCaptor<OutboxEvent> saved = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(OutboxEvent::getTopic).containsOnly("tuevento.order");
        assertThat(saved.getAllValues()).extracting(OutboxEvent::getAggregateType).containsOnly("order");
    }

    @Test
    @DisplayName("sin tipo de agregado o de evento falla sin guardar nada")
    void blankTypes_failWithoutSaving() {
        OutboxEventPublisher publisher = new OutboxEventPublisher(outboxEventRepository, new ObjectMapper());

        assertThatThrownBy(() -> publisher.publish(" ", "9", "OrderPaid", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> publisher.publish(null, "9", "OrderPaid", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> publisher.publish("order", "9", "", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("sin eventId falla sin guardar nada")
    void nullEventId_failsWithoutSaving() {
        OutboxEventPublisher publisher = new OutboxEventPublisher(outboxEventRepository, new ObjectMapper());

        assertThatThrownBy(() -> publisher.publish("order", "9", "OrderPaid", Map.of("orderId", 9), null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("un payload que no se puede serializar falla sin guardar nada")
    void unserializablePayload_failsWithoutSaving() {
        OutboxEventPublisher publisher = new OutboxEventPublisher(outboxEventRepository, new ObjectMapper());

        assertThatThrownBy(() -> publisher.publish("order", "9", "OrderPaid", new Object()))
                .isInstanceOf(IllegalArgumentException.class);
        verify(outboxEventRepository, never()).save(any());
    }
}
