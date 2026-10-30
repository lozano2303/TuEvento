package com.capysoft.tuevento.modules.ticket.application.usecase;

import com.capysoft.tuevento.modules.ticket.application.dto.response.TicketQrResponse;
import com.capysoft.tuevento.modules.ticket.application.port.out.EventLookupPort;
import com.capysoft.tuevento.modules.ticket.application.port.out.EventLookupPort.EventRef;
import com.capysoft.tuevento.modules.ticket.domain.model.Order;
import com.capysoft.tuevento.modules.ticket.domain.model.Ticket;
import com.capysoft.tuevento.modules.ticket.domain.model.TicketStatus;
import com.capysoft.tuevento.modules.ticket.domain.port.QrNonceStore;
import com.capysoft.tuevento.modules.ticket.domain.port.QrTokenPort;
import com.capysoft.tuevento.modules.ticket.domain.repository.OrderRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Solo un evento publicado (según Catálogo) permite generar el QR de una entrada o una orden. */
@ExtendWith(MockitoExtension.class)
@DisplayName("Generar QR — estado del evento")
class GenerateQrEventStatusTest {

    private static final Long USER_ID = 7L;
    private static final Long EVENT_ID = 300L;
    private static final Long TICKET_ID = 50L;
    private static final Long ORDER_ID = 40L;

    @Mock private OrderRepository orderRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private EventLookupPort eventLookupPort;
    @Mock private QrTokenPort qrTokenPort;
    @Mock private QrNonceStore qrNonceStore;

    private static EventRef event(String status) {
        return new EventRef(EVENT_ID, 30L, "Concierto", status);
    }

    private static Ticket paidTicket() {
        return Ticket.builder().ticketId(TICKET_ID).orderId(ORDER_ID).eventId(EVENT_ID).userId(USER_ID)
                .status(TicketStatus.PAID).build();
    }

    @Nested
    @DisplayName("QR de una entrada")
    class TicketQr {

        private GenerateTicketQrUseCaseImpl useCase;

        @BeforeEach
        void setUp() {
            useCase = new GenerateTicketQrUseCaseImpl(ticketRepository, eventLookupPort, qrTokenPort, qrNonceStore);
        }

        @Test
        @DisplayName("evento publicado → genera el token con el evento de la entrada")
        void published_generatesToken() {
            when(ticketRepository.findById(TICKET_ID)).thenReturn(Optional.of(paidTicket()));
            when(eventLookupPort.find(EVENT_ID)).thenReturn(Optional.of(event("PUBLISHED")));
            when(qrTokenPort.getExpirationSeconds()).thenReturn(60L);
            when(qrTokenPort.generate(any(), anyLong(), anyLong(), anyLong(), anyString())).thenReturn("token");

            TicketQrResponse response = useCase.execute(TICKET_ID, USER_ID);

            assertThat(response.getQrToken()).isEqualTo("token");
            assertThat(response.getTargetId()).isEqualTo(TICKET_ID);
        }

        @Test
        @DisplayName("evento no publicado → EVENT_NOT_ACTIVE con su estado")
        void notPublished_rejected() {
            when(ticketRepository.findById(TICKET_ID)).thenReturn(Optional.of(paidTicket()));
            when(eventLookupPort.find(EVENT_ID)).thenReturn(Optional.of(event("CANCELLED")));

            assertThatThrownBy(() -> useCase.execute(TICKET_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("CANCELLED")
                    .extracting(e -> ((BusinessException) e).getCode()).isEqualTo("EVENT_NOT_ACTIVE");
            verify(qrNonceStore, never()).store(any(), anyLong(), anyString(), anyLong());
        }

        @Test
        @DisplayName("evento inexistente → EVENT_NOT_FOUND")
        void missingEvent_notFound() {
            when(ticketRepository.findById(TICKET_ID)).thenReturn(Optional.of(paidTicket()));
            when(eventLookupPort.find(EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.execute(TICKET_ID, USER_ID)).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("QR de una orden")
    class OrderQr {

        private GenerateOrderQrUseCaseImpl useCase;

        @BeforeEach
        void setUp() {
            useCase = new GenerateOrderQrUseCaseImpl(orderRepository, ticketRepository, eventLookupPort,
                    qrTokenPort, qrNonceStore);
        }

        private void givenOrder() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(
                    Order.builder().orderId(ORDER_ID).eventId(EVENT_ID).userId(USER_ID).build()));
        }

        @Test
        @DisplayName("evento publicado y entradas pagadas → genera el token")
        void published_generatesToken() {
            givenOrder();
            when(eventLookupPort.find(EVENT_ID)).thenReturn(Optional.of(event("PUBLISHED")));
            when(ticketRepository.findByOrderId(ORDER_ID)).thenReturn(List.of(paidTicket()));
            when(qrTokenPort.getExpirationSeconds()).thenReturn(60L);
            when(qrTokenPort.generate(any(), anyLong(), anyLong(), anyLong(), anyString())).thenReturn("token");

            assertThat(useCase.execute(ORDER_ID, USER_ID).getQrToken()).isEqualTo("token");
        }

        @Test
        @DisplayName("evento no publicado → EVENT_NOT_ACTIVE")
        void notPublished_rejected() {
            givenOrder();
            when(eventLookupPort.find(EVENT_ID)).thenReturn(Optional.of(event("COMPLETED")));

            assertThatThrownBy(() -> useCase.execute(ORDER_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("COMPLETED");
            verify(ticketRepository, never()).findByOrderId(anyLong());
        }

        @Test
        @DisplayName("evento inexistente → EVENT_NOT_FOUND")
        void missingEvent_notFound() {
            givenOrder();
            when(eventLookupPort.find(EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.execute(ORDER_ID, USER_ID)).isInstanceOf(NotFoundException.class);
        }
    }
}
