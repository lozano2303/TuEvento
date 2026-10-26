package com.capysoft.tuevento.shared.infrastructure.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Map;

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
    @DisplayName("guarda el payload como JSON en el topic tuevento.<agregado>.<evento-en-kebab>")
    void savesJsonPayloadWithDerivedTopic() {
        OutboxEventPublisher publisher = new OutboxEventPublisher(outboxEventRepository, new ObjectMapper());

        publisher.publish("Order", "9", "OrderPaid", Map.of("orderId", 9));

        ArgumentCaptor<OutboxEvent> saved = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(saved.capture());
        OutboxEvent event = saved.getValue();
        assertThat(event.getTopic()).isEqualTo("tuevento.order.order-paid");
        assertThat(event.getPayload()).isEqualTo("{\"orderId\":9}");
        assertThat(event.getAggregateId()).isEqualTo("9");
        assertThat(event.isProcessed()).isFalse();
        assertThat(event.getCreatedAt()).isNotNull();
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
