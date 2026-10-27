package com.capysoft.tuevento.modules.event.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.capysoft.tuevento.modules.event.application.dto.request.ChangeEventStatusRequest;
import com.capysoft.tuevento.modules.event.application.port.in.AdminChangeEventStatusPort;
import com.capysoft.tuevento.modules.event.domain.event.EventStatusChangedEvent;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("CloseRevokedOrganizerEventsUseCase")
class CloseRevokedOrganizerEventsUseCaseTest {

    private static final Integer ORGANIZER = 10;
    private static final Integer ADMIN = 1;
    private static final String REASON = "Incumplimiento de las normas";

    @Mock EventRepository eventRepository;
    @Mock AdminChangeEventStatusPort adminChangeEventStatusPort;
    @InjectMocks CloseRevokedOrganizerEventsUseCase useCase;

    private static Event event(long id, EventStatus status) {
        return Event.builder().eventId(id).userId(ORGANIZER.longValue()).status(status).build();
    }

    @Test
    @DisplayName("cancela los PUBLISHED y rechaza los PENDING_REVIEW, con el motivo y la causa ORGANIZER_REVOKED")
    void closesActiveEvents() {
        when(eventRepository.findByUserId(10L)).thenReturn(List.of(
                event(100, EventStatus.PUBLISHED), event(101, EventStatus.PENDING_REVIEW),
                event(102, EventStatus.DRAFT), event(103, EventStatus.CANCELLED), event(104, EventStatus.COMPLETED)));

        int closed = useCase.close(ORGANIZER, ADMIN, REASON);

        assertThat(closed).isEqualTo(2);
        ArgumentCaptor<ChangeEventStatusRequest> request = ArgumentCaptor.forClass(ChangeEventStatusRequest.class);
        verify(adminChangeEventStatusPort).execute(eq(100L), request.capture(), eq(1L),
                eq(EventStatusChangedEvent.CAUSE_ORGANIZER_REVOKED));
        assertThat(request.getValue().getNewStatus()).isEqualTo(EventStatus.CANCELLED);
        assertThat(request.getValue().getReason()).isEqualTo("Rol de organizador revocado: " + REASON);
        verify(adminChangeEventStatusPort).execute(eq(101L), request.capture(), eq(1L),
                eq(EventStatusChangedEvent.CAUSE_ORGANIZER_REVOKED));
        assertThat(request.getValue().getNewStatus()).isEqualTo(EventStatus.REJECTED);
        assertThat(request.getValue().getReason()).isEqualTo("Se revocó tu rol de organizador: " + REASON);
        verify(adminChangeEventStatusPort, never()).execute(eq(102L), any(), any(), any());
        verify(adminChangeEventStatusPort, never()).execute(eq(103L), any(), any(), any());
        verify(adminChangeEventStatusPort, never()).execute(eq(104L), any(), any(), any());
    }

    @Test
    @DisplayName("repetirlo no cierra nada otra vez: los eventos ya cerrados se saltan")
    void repeatingIsHarmless() {
        when(eventRepository.findByUserId(10L)).thenReturn(List.of(
                event(100, EventStatus.CANCELLED), event(101, EventStatus.REJECTED)));

        assertThat(useCase.close(ORGANIZER, ADMIN, REASON)).isZero();
        verifyNoInteractions(adminChangeEventStatusPort);
    }

    @Test
    @DisplayName("si uno falla, cierra los demás y lanza para que se reintente")
    void failureOfOneStillClosesOthersAndThrows() {
        when(eventRepository.findByUserId(10L)).thenReturn(List.of(
                event(100, EventStatus.PUBLISHED), event(101, EventStatus.PUBLISHED)));
        when(adminChangeEventStatusPort.execute(eq(100L), any(), any(), any()))
                .thenThrow(new IllegalStateException("db timeout"));

        assertThatThrownBy(() -> useCase.close(ORGANIZER, ADMIN, REASON))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("[100]");
        verify(adminChangeEventStatusPort).execute(eq(101L), any(), eq(1L), any());
    }

    @Test
    @DisplayName("sin admin conocido la auditoría queda sin autor")
    void nullAdmin() {
        when(eventRepository.findByUserId(10L)).thenReturn(List.of(event(100, EventStatus.PUBLISHED)));

        useCase.close(ORGANIZER, null, REASON);

        verify(adminChangeEventStatusPort).execute(eq(100L), any(), eq(null),
                eq(EventStatusChangedEvent.CAUSE_ORGANIZER_REVOKED));
    }
}
