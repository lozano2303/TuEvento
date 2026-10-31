package com.capysoft.commerce.payment.infrastructure.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.capysoft.commerce.payment.application.port.in.RefundRejectedSaleUseCase;
import com.capysoft.commerce.shared.messaging.DomainEventsConsumerConfig;
import com.capysoft.commerce.shared.messaging.InboundEventRecords;
import com.capysoft.commerce.shared.messaging.InboundEventRecords.Inbound;
import com.capysoft.commerce.shared.messaging.ProcessedEventStore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** {@code SeatsSaleRejected} de {@code tuevento.seat}: paso 4 de la saga cuando la venta falla. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "messaging.consumers.enabled", havingValue = "true", matchIfMissing = true)
public class SeatsSaleRejectedKafkaListener {

    static final String TOPIC = "tuevento.seat";
    static final String SEATS_SALE_REJECTED = "SeatsSaleRejected";
    static final String CONSUMER = "comercio.seats-sale-rejected";

    private final RefundRejectedSaleUseCase refundRejectedSale;
    private final ProcessedEventStore processedEventStore;
    private final ObjectMapper objectMapper;

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(Long orderId, String reason) {
    }

    @KafkaListener(topics = TOPIC, groupId = "comercio-seats-sale-rejected",
            containerFactory = DomainEventsConsumerConfig.CONTAINER_FACTORY)
    public void onMessage(ConsumerRecord<String, String> rec) {
        Inbound<Message> inbound = InboundEventRecords.readNew(rec, SEATS_SALE_REJECTED, CONSUMER,
                processedEventStore, objectMapper, Message.class).orElse(null);
        if (inbound == null) {
            return;
        }
        if (inbound.payload().orderId() == null) {
            log.error("Discarding SeatsSaleRejected without orderId: offset={}", rec.offset());
            return;
        }
        refundRejectedSale.execute(inbound.payload().orderId(), inbound.payload().reason());
        if (inbound.eventId() != null) {
            processedEventStore.markProcessed(CONSUMER, inbound.eventId());
        }
    }
}
