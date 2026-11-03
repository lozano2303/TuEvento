package com.capysoft.tuevento.modules.seat.infrastructure.client;

import java.util.Collection;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.catalogbridge.client.CommerceServiceClient;
import com.capysoft.tuevento.modules.seat.application.port.out.PendingPaymentSeatsPort;

import lombok.RequiredArgsConstructor;

/**
 * Asks commerce-service which seats have an order waiting for its payment. If Commerce does not
 * answer the call fails, so the scheduler releases nothing instead of freeing seats being paid.
 */
@Component
@RequiredArgsConstructor
class PendingPaymentSeatsAdapter implements PendingPaymentSeatsPort {

    private final CommerceServiceClient commerceServiceClient;

    @Override
    public Set<Integer> findSeatIdsWithPendingPayment(Collection<Integer> seatIds) {
        return commerceServiceClient.seatsWithPendingPayment(seatIds);
    }
}
