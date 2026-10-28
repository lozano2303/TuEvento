package com.capysoft.tuevento.modules.event.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.capysoft.tuevento.modules.category.application.port.in.CategoryEventUseCase;
import com.capysoft.tuevento.modules.event.application.dto.response.AdminEventSummaryResponse;
import com.capysoft.tuevento.modules.event.application.port.out.UserDirectoryPort;
import com.capysoft.tuevento.modules.event.application.port.out.UserDirectoryPort.UserRef;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.repository.EventMediaRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.event.application.port.out.SiteLookupPort;
import com.capysoft.tuevento.shared.application.storage.FileStorage;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GetAdminEventsUseCase — organizadores")
class GetAdminEventsUseCaseTest {

    @Mock EventRepository eventRepository;
    @Mock EventMediaRepository eventMediaRepository;
    @Mock CategoryEventUseCase categoryEventUseCase;
    @Mock SiteLookupPort siteLookupPort;
    @Mock UserDirectoryPort userDirectoryPort;
    @Mock FileStorage fileStorage;
    @InjectMocks GetAdminEventsUseCase useCase;

    private static Event event(long eventId, long organizerId) {
        return Event.builder().eventId(eventId).userId(organizerId).eventName("E" + eventId)
                .siteId(1L).status(EventStatus.PUBLISHED).build();
    }

    @Test
    @DisplayName("resuelve los organizadores de todos los eventos en una sola consulta a Identidad")
    @SuppressWarnings("unchecked")
    void resolvesOrganizersInOneBatch() {
        when(eventRepository.findByStatus(EventStatus.PUBLISHED))
                .thenReturn(List.of(event(1, 7), event(2, 7), event(3, 8)));
        when(eventMediaRepository.findFirstByEventId(anyLong())).thenReturn(Optional.empty());
        when(categoryEventUseCase.getCategoriesByEvent(anyInt())).thenReturn(List.of());
        when(userDirectoryPort.findUsers(anyCollection())).thenReturn(Map.of(
                7, new UserRef(7, "Ana", "ana@test.com", 15),
                8, new UserRef(8, null, null, null)));
        when(fileStorage.findPublicUrl(15)).thenReturn(Optional.of("https://cdn/ana.png"));

        List<AdminEventSummaryResponse> events = useCase.getEvents(EventStatus.PUBLISHED);

        ArgumentCaptor<Collection<Integer>> ids = ArgumentCaptor.forClass(Collection.class);
        verify(userDirectoryPort, times(1)).findUsers(ids.capture());
        assertThat(ids.getValue()).containsExactlyInAnyOrder(7, 8);
        // Una sola petición por foto distinta, aunque Ana organice dos eventos
        verify(fileStorage, times(1)).findPublicUrl(15);

        assertThat(events.get(0).getOrganizerName()).isEqualTo("Ana");
        assertThat(events.get(0).getOrganizerEmail()).isEqualTo("ana@test.com");
        assertThat(events.get(0).getOrganizerProfilePicture()).isEqualTo("https://cdn/ana.png");
        assertThat(events.get(2).getOrganizerName()).isNull();
        assertThat(events.get(2).getOrganizerProfilePicture()).isNull();
    }

    @Test
    @DisplayName("si Identidad falla, la lista sale igual sin datos del organizador")
    void identityFailureIsSoft() {
        when(eventRepository.findAll()).thenReturn(List.of(event(1, 7)));
        when(eventMediaRepository.findFirstByEventId(anyLong())).thenReturn(Optional.empty());
        when(categoryEventUseCase.getCategoriesByEvent(anyInt())).thenReturn(List.of());
        when(userDirectoryPort.findUsers(any())).thenThrow(new IllegalStateException("identity down"));

        List<AdminEventSummaryResponse> events = useCase.getEvents(null);

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getOrganizerName()).isNull();
        assertThat(events.get(0).getOrganizerUserId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("si el servicio de archivos falla, la lista sale igual sin la foto del organizador")
    void storageFailureIsSoft() {
        when(eventRepository.findAll()).thenReturn(List.of(event(1, 7)));
        when(eventMediaRepository.findFirstByEventId(anyLong())).thenReturn(Optional.empty());
        when(categoryEventUseCase.getCategoriesByEvent(anyInt())).thenReturn(List.of());
        when(userDirectoryPort.findUsers(anyCollection())).thenReturn(Map.of(7, new UserRef(7, "Ana", "a@t.com", 15)));
        when(fileStorage.findPublicUrl(15)).thenThrow(new IllegalStateException("storage down"));

        List<AdminEventSummaryResponse> events = useCase.getEvents(null);

        assertThat(events.get(0).getOrganizerName()).isEqualTo("Ana");
        assertThat(events.get(0).getOrganizerProfilePicture()).isNull();
    }
}
