package com.capysoft.commerce.ticket.infrastructure.client;

import java.util.List;

import org.springframework.stereotype.Component;

import com.capysoft.commerce.infrastructure.client.CatalogServiceClient;
import com.capysoft.commerce.shared.outbox.OutboxEventPublisher;
import com.capysoft.commerce.ticket.application.port.out.CatalogSeatsPort;

import lombok.RequiredArgsConstructor;

/** Los pasos de la saga de compra hacia Catálogo: el bloqueo por REST, el resto por el outbox. */
@Component
@RequiredArgsConstructor
class CatalogSeatsAdapter implements CatalogSeatsPort {

    static final String AGGREGATE = "order";
    static final String ORDER_PAID = "OrderPaid";
    static final String ORDER_RELEASED = "OrderReleased";

    private final CatalogServiceClient catalog;
    private final OutboxEventPublisher outbox;

    record OrderPaid(Long orderId, Long userId, Long eventId, List<Integer> seatIds, String providerPaymentId) {
    }

    /** {@code userId} y {@code seatIds} amplían el contrato: Catálogo libera sin buscar por pending_order_id. */
    record OrderReleased(Long orderId, Long userId, List<Integer> seatIds, ReleaseReason reason, String detail,
                         Integer changedBy) {
    }

    @Override
    public HeldSeats holdForOrder(Long orderId, Long userId, Long eventId, List<Integer> seatIds) {
        CatalogServiceClient.HeldSeats held = catalog.hold(orderId, userId, eventId, seatIds);
        return new HeldSeats(held.currency(), held.seats().stream()
                .map(s -> new HeldSeat(s.seatId(), s.eventSectionId(), s.price()))
                .toList());
    }

    @Override
    public void sell(Long orderId, Long buyerId, Long eventId, List<Integer> seatIds, String providerPaymentId) {
        outbox.publish(AGGREGATE, String.valueOf(orderId), ORDER_PAID,
                new OrderPaid(orderId, buyerId, eventId, seatIds, providerPaymentId));
    }

    @Override
    public void release(Long orderId, Long buyerId, List<Integer> seatIds, Integer changedBy, ReleaseReason reason,
                        String detail) {
        outbox.publish(AGGREGATE, String.valueOf(orderId), ORDER_RELEASED,
                new OrderReleased(orderId, buyerId, seatIds, reason, detail, changedBy));
    }
}
