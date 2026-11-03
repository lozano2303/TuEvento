package com.capysoft.tuevento.modules.catalogbridge.messaging;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.catalogbridge.application.OrderSeatsSagaService;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.infrastructure.config.DomainEventsConsumerConfig;
import com.capysoft.tuevento.shared.infrastructure.messaging.InboundEventRecords;
import com.capysoft.tuevento.shared.infrastructure.messaging.InboundEventRecords.Inbound;
import com.capysoft.tuevento.shared.infrastructure.messaging.ProcessedEventStore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * commerce-service publica el resultado de cada orden en {@code tuevento.order}. La venta no depende
 * de que el monolito esté arriba al llegar el webhook: el mensaje espera aquí. Un error que no sea de
 * negocio se propaga para que el contenedor reintente; los mensajes de la misma orden esperan.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.consumers.enabled", havingValue = "true", matchIfMissing = true)
public class OrderEventsKafkaListener {

    static final String TOPIC = "tuevento.order";
    static final String ORDER_PAID = "OrderPaid";
    static final String ORDER_RELEASED = "OrderReleased";

    private final OrderSeatsSagaService saga;
    private final ProcessedEventStore processedEventStore;
    private final ObjectMapper objectMapper;

    /** Lleva los campos de OrderPaid y de OrderReleased: el tipo va en el header. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(Long orderId, Long userId, List<Integer> seatIds, String providerPaymentId, String reason,
                   String detail, Integer changedBy) {
    }

    @KafkaListener(topics = TOPIC, groupId = "catalogo-order-seats",
            containerFactory = DomainEventsConsumerConfig.CONTAINER_FACTORY)
    public void onMessage(ConsumerRecord<String, String> rec) {
        Inbound<Message> inbound = InboundEventRecords.readNew(rec, Set.of(ORDER_PAID, ORDER_RELEASED),
                OrderSeatsSagaService.CONSUMER, processedEventStore, objectMapper, Message.class).orElse(null);
        if (inbound == null) {
            return;
        }
        Message message = inbound.payload();
        if (message.orderId() == null || message.userId() == null || message.seatIds() == null) {
            log.error("Discarding {} without orderId, userId or seatIds: offset={}", inbound.eventType(), rec.offset());
            return;
        }
        if (ORDER_PAID.equals(inbound.eventType())) {
            sell(inbound.eventId(), message);
        } else {
            String reason = message.detail() != null ? message.detail() : message.reason();
            saga.release(inbound.eventId(), message.orderId(), message.userId(), message.seatIds(), message.changedBy(), reason);
        }
    }

    private void sell(UUID eventId, Message message) {
        try {
            saga.sell(eventId, message.orderId(), message.userId(), message.seatIds(),
                    "Payment confirmed: " + message.providerPaymentId());
        } catch (BusinessException e) {
            // La transacción de la venta ya se deshizo: rechazarla va en una nueva
            saga.rejectSale(eventId, message.orderId(), message.seatIds(), e);
        }
    }
}
