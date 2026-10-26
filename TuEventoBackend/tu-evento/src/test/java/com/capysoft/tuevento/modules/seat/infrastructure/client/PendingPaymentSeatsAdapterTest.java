package com.capysoft.tuevento.modules.seat.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.capysoft.tuevento.modules.ticket.application.port.in.FindSeatsWithPendingPaymentUseCase;
import com.capysoft.tuevento.modules.ticket.application.usecase.FindSeatsWithPendingPaymentUseCaseImpl;
import com.capysoft.tuevento.modules.ticket.domain.model.OrderStatus;
import com.capysoft.tuevento.modules.ticket.domain.repository.SeatTicketRepository;

@DisplayName("Sillas con pago pendiente — puerto de seat y caso de uso de ticket")
class PendingPaymentSeatsAdapterTest {

    @Test
    @DisplayName("el adaptador de seat delega en ticket, que filtra por órdenes en PAYMENT_PENDING")
    void adapterDelegatesToTicketUseCase() {
        SeatTicketRepository seatTickets = mock(SeatTicketRepository.class);
        when(seatTickets.findSeatIdsByOrderStatus(List.of(1, 2), OrderStatus.PAYMENT_PENDING)).thenReturn(Set.of(2));
        FindSeatsWithPendingPaymentUseCase useCase = new FindSeatsWithPendingPaymentUseCaseImpl(seatTickets);

        assertThat(new PendingPaymentSeatsAdapter(useCase).findSeatIdsWithPendingPayment(List.of(1, 2)))
                .containsExactly(2);
    }
}
