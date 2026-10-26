package com.capysoft.tuevento.modules.ticket.application.usecase;

import java.util.Collection;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.tuevento.modules.ticket.application.port.in.FindSeatsWithPendingPaymentUseCase;
import com.capysoft.tuevento.modules.ticket.domain.model.OrderStatus;
import com.capysoft.tuevento.modules.ticket.domain.repository.SeatTicketRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FindSeatsWithPendingPaymentUseCaseImpl implements FindSeatsWithPendingPaymentUseCase {

    private final SeatTicketRepository seatTicketRepository;

    @Override
    @Transactional(readOnly = true)
    public Set<Integer> findSeatIdsWithPendingPayment(Collection<Integer> seatIds) {
        return seatTicketRepository.findSeatIdsByOrderStatus(seatIds, OrderStatus.PAYMENT_PENDING);
    }
}
