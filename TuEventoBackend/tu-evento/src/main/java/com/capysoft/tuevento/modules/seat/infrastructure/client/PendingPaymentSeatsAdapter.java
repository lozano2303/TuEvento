package com.capysoft.tuevento.modules.seat.infrastructure.client;

import java.util.Collection;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.seat.application.port.out.PendingPaymentSeatsPort;
import com.capysoft.tuevento.modules.ticket.application.port.in.FindSeatsWithPendingPaymentUseCase;

import lombok.RequiredArgsConstructor;

/**
 * Único punto del módulo seat que conoce a ticket. Hoy delega en el caso de uso del
 * monolito; al separar Comercio se sustituye por un cliente REST.
 */
@Component
@RequiredArgsConstructor
class PendingPaymentSeatsAdapter implements PendingPaymentSeatsPort {

    private final FindSeatsWithPendingPaymentUseCase findSeatsWithPendingPaymentUseCase;

    @Override
    public Set<Integer> findSeatIdsWithPendingPayment(Collection<Integer> seatIds) {
        return findSeatsWithPendingPaymentUseCase.findSeatIdsWithPendingPayment(seatIds);
    }
}
