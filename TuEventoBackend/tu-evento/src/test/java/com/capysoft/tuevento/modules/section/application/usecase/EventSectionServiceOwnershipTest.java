package com.capysoft.tuevento.modules.section.application.usecase;

import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.modules.language.domain.repository.SectionTypeTranslationRepository;
import com.capysoft.tuevento.modules.section.application.dto.request.CreateEventSectionRequest;
import com.capysoft.tuevento.modules.section.application.dto.request.UpdateEventSectionRequest;
import com.capysoft.tuevento.modules.section.application.dto.response.EventSectionResponse;
import com.capysoft.tuevento.modules.section.domain.model.EventSection;
import com.capysoft.tuevento.modules.section.domain.model.SectionType;
import com.capysoft.tuevento.modules.section.domain.repository.EventSectionRepository;
import com.capysoft.tuevento.modules.section.domain.repository.SectionTypeRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.ForbiddenException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Solo el organizador dueño del evento puede crear, modificar o borrar sus secciones,
 * y solo mientras el evento está en DRAFT.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventSectionService — propiedad del evento")
class EventSectionServiceOwnershipTest {

    private static final int EVENT_ID = 5;
    private static final long OWNER_ID = 9L;
    private static final long OTHER_ID = 77L;
    private static final int SECTION_ID = 30;
    private static final int SECTION_TYPE_ID = 2;

    @Mock EventSectionRepository eventSectionRepository;
    @Mock SectionTypeRepository sectionTypeRepository;
    @Mock SectionTypeTranslationRepository sectionTypeTranslationRepository;
    @Mock LanguageRepository languageRepository;
    @Mock EventRepository eventRepository;

    @InjectMocks EventSectionService service;

    private void eventIs(EventStatus status) {
        when(eventRepository.findById((long) EVENT_ID)).thenReturn(Optional.of(Event.builder()
                .eventId((long) EVENT_ID).userId(OWNER_ID).status(status).build()));
    }

    private void existingSection() {
        when(eventSectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(EventSection.builder()
                .eventSectionId(SECTION_ID).eventId(EVENT_ID).sectionTypeId(SECTION_TYPE_ID)
                .capacity(100).availableSeats(100).price(BigDecimal.TEN).isActive(true).build()));
    }

    private void sectionTypeExists() {
        when(sectionTypeRepository.findById(SECTION_TYPE_ID))
                .thenReturn(Optional.of(SectionType.builder().sectionTypeId(SECTION_TYPE_ID).name("VIP").build()));
    }

    private static CreateEventSectionRequest createRequest() {
        return CreateEventSectionRequest.builder()
                .eventId(EVENT_ID).sectionTypeId(SECTION_TYPE_ID).capacity(50).price(BigDecimal.valueOf(20000)).build();
    }

    @Nested
    @DisplayName("Crear")
    class Create {

        @Test
        @DisplayName("el dueño en DRAFT crea la sección")
        void ownerCreates() {
            eventIs(EventStatus.DRAFT);
            sectionTypeExists();
            when(eventSectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            EventSectionResponse response = service.createEventSection(createRequest(), OWNER_ID);

            assertThat(response.getCapacity()).isEqualTo(50);
            assertThat(response.getSectionTypeName()).isEqualTo("VIP");
        }

        @Test
        @DisplayName("otro organizador recibe EVENT_ACCESS_DENIED y no se guarda nada")
        void otherOrganizerIsForbidden() {
            eventIs(EventStatus.DRAFT);

            assertThatThrownBy(() -> service.createEventSection(createRequest(), OTHER_ID))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("No eres el organizador");
            verify(eventSectionRepository, never()).save(any());
        }

        @Test
        @DisplayName("un evento publicado no admite secciones nuevas")
        void publishedEventIsRejected() {
            eventIs(EventStatus.PUBLISHED);

            assertThatThrownBy(() -> service.createEventSection(createRequest(), OWNER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("DRAFT");
            verify(eventSectionRepository, never()).save(any());
        }

        @Test
        @DisplayName("un evento inexistente responde EVENT_NOT_FOUND")
        void missingEvent() {
            when(eventRepository.findById((long) EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createEventSection(createRequest(), OWNER_ID))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Modificar")
    class Update {

        @Test
        @DisplayName("el dueño cambia precio y aforo")
        void ownerUpdates() {
            existingSection();
            eventIs(EventStatus.DRAFT);
            sectionTypeExists();
            when(eventSectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            EventSectionResponse response = service.updateEventSection(SECTION_ID,
                    UpdateEventSectionRequest.builder().capacity(80).price(BigDecimal.ONE).build(), OWNER_ID);

            assertThat(response.getCapacity()).isEqualTo(80);
            assertThat(response.getPrice()).isEqualByComparingTo(BigDecimal.ONE);
            // Las disponibles bajan lo mismo que el aforo (antes se quedaban en 100)
            assertThat(response.getAvailableSeats()).isEqualTo(80);
        }

        @Test
        @DisplayName("otro organizador no puede cambiar el precio de un evento ajeno")
        void otherOrganizerIsForbidden() {
            existingSection();
            eventIs(EventStatus.DRAFT);

            assertThatThrownBy(() -> service.updateEventSection(SECTION_ID,
                    UpdateEventSectionRequest.builder().price(BigDecimal.ONE).build(), OTHER_ID))
                    .isInstanceOf(ForbiddenException.class);
            verify(eventSectionRepository, never()).save(any());
        }

        @Test
        @DisplayName("con el evento publicado tampoco el dueño cambia precios")
        void publishedEventIsRejected() {
            existingSection();
            eventIs(EventStatus.PUBLISHED);

            assertThatThrownBy(() -> service.updateEventSection(SECTION_ID,
                    UpdateEventSectionRequest.builder().price(BigDecimal.ONE).build(), OWNER_ID))
                    .isInstanceOf(BusinessException.class);
            verify(eventSectionRepository, never()).save(any());
        }

        @Test
        @DisplayName("una sección inexistente responde EVENT_SECTION_NOT_FOUND")
        void missingSection() {
            when(eventSectionRepository.findById(SECTION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateEventSection(SECTION_ID,
                    UpdateEventSectionRequest.builder().build(), OWNER_ID))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Borrar")
    class Delete {

        @Test
        @DisplayName("el dueño en DRAFT borra la sección")
        void ownerDeletes() {
            existingSection();
            eventIs(EventStatus.DRAFT);

            service.deleteEventSection(SECTION_ID, OWNER_ID);

            verify(eventSectionRepository).deleteById(SECTION_ID);
        }

        @Test
        @DisplayName("otro organizador no puede borrar secciones de un evento ajeno")
        void otherOrganizerIsForbidden() {
            existingSection();
            eventIs(EventStatus.DRAFT);

            assertThatThrownBy(() -> service.deleteEventSection(SECTION_ID, OTHER_ID))
                    .isInstanceOf(ForbiddenException.class);
            verify(eventSectionRepository, never()).deleteById(anyInt());
        }

        @Test
        @DisplayName("una sección inexistente responde EVENT_SECTION_NOT_FOUND")
        void missingSection() {
            when(eventSectionRepository.findById(SECTION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteEventSection(SECTION_ID, OWNER_ID))
                    .isInstanceOf(NotFoundException.class);
            verify(eventSectionRepository, never()).deleteById(anyInt());
        }
    }
}
