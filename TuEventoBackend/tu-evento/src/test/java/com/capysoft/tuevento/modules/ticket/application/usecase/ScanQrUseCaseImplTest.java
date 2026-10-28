package com.capysoft.tuevento.modules.ticket.application.usecase;

import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.ticket.application.dto.response.ScanResultResponse;
import com.capysoft.tuevento.modules.ticket.domain.event.TicketsValidatedEvent;
import com.capysoft.tuevento.modules.ticket.domain.model.Ticket;
import com.capysoft.tuevento.modules.ticket.domain.model.TicketStatus;
import com.capysoft.tuevento.modules.ticket.domain.port.QrNonceStore;
import com.capysoft.tuevento.modules.ticket.domain.port.QrTokenPort;
import com.capysoft.tuevento.modules.ticket.domain.port.QrTokenPort.QrPayload;
import com.capysoft.tuevento.modules.ticket.domain.port.QrTokenPort.QrScope;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketCheckinRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketLogRepository;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScanQrUseCaseImpl — validación de QR y aviso al asistente")
class ScanQrUseCaseImplTest {

    private static final Long EVENT_ID = 7L;
    private static final Long ORGANIZER_ID = 1L;
    private static final Long OWNER_ID = 50L;
    private static final Long ORDER_ID = 300L;

    @Mock private QrTokenPort qrTokenPort;
    @Mock private QrNonceStore qrNonceStore;
    @Mock private TicketRepository ticketRepository;
    @Mock private TicketCheckinRepository ticketCheckinRepository;
    @Mock private TicketLogRepository ticketLogRepository;
    @Mock private EventRepository eventRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private ScanQrUseCaseImpl useCase;

    private static Ticket ticket(long id, TicketStatus status) {
        return Ticket.builder()
                .ticketId(id).eventId(EVENT_ID).userId(OWNER_ID).orderId(ORDER_ID).code("T-" + id)
                .status(status).expirationDate(LocalDateTime.now().plusDays(1))
                .build();
    }

    private void qr(QrScope scope, Long targetId) {
        QrPayload payload = new QrPayload(scope, targetId, EVENT_ID, OWNER_ID, "nonce");
        when(qrTokenPort.parse("token")).thenReturn(Optional.of(payload));
        when(qrNonceStore.consume(scope, targetId, "nonce")).thenReturn(true);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(Event.builder().userId(ORGANIZER_ID).build()));
    }

    @Test
    @DisplayName("validar una orden marca sus tickets y avisa una vez a su dueño")
    void validatingAnOrderNotifiesItsOwner() {
        qr(QrScope.ORDER, ORDER_ID);
        when(ticketRepository.findByOrderIdForUpdate(ORDER_ID))
                .thenReturn(List.of(ticket(11, TicketStatus.PAID), ticket(12, TicketStatus.PAID)));

        ScanResultResponse result = useCase.execute("token", ORGANIZER_ID);

        assertThat(result.getValidatedCount()).isEqualTo(2);
        ArgumentCaptor<TicketsValidatedEvent> event = ArgumentCaptor.forClass(TicketsValidatedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(event.capture());
        assertThat(event.getValue().userId()).isEqualTo(OWNER_ID);
        assertThat(event.getValue().scope()).isEqualTo("ORDER");
        assertThat(event.getValue().targetId()).isEqualTo(ORDER_ID);
        assertThat(event.getValue().eventId()).isEqualTo(EVENT_ID);
        assertThat(event.getValue().ticketIds()).containsExactly(11L, 12L);
    }

    @Test
    @DisplayName("un QR con tickets ya usados no vuelve a avisar")
    void alreadyUsedTicketsDoNotNotify() {
        qr(QrScope.TICKET, 11L);
        when(ticketRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(ticket(11, TicketStatus.USED)));

        ScanResultResponse result = useCase.execute("token", ORGANIZER_ID);

        assertThat(result.getAlreadyUsedCount()).isEqualTo(1);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("un QR ya consumido se rechaza sin tocar tickets ni avisar")
    void replacedQrIsRejected() {
        when(qrTokenPort.parse("token"))
                .thenReturn(Optional.of(new QrPayload(QrScope.TICKET, 11L, EVENT_ID, OWNER_ID, "nonce")));
        when(qrNonceStore.consume(QrScope.TICKET, 11L, "nonce")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute("token", ORGANIZER_ID))
                .isInstanceOf(BusinessException.class);
        verify(ticketRepository, never()).findByIdForUpdate(anyLong());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
