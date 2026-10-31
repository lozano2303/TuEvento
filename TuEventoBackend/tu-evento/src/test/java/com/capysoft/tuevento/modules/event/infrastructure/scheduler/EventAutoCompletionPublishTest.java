package com.capysoft.tuevento.modules.event.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.capysoft.tuevento.modules.event.domain.event.EventStatusChangedEvent;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventStatusLogRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventAutoCompletionScheduler — avisa a quien replica el estado")
class EventAutoCompletionPublishTest {

    @Mock EventRepository eventRepository;
    @Mock EventStatusLogRepository eventStatusLogRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks EventAutoCompletionScheduler scheduler;

    @Test
    @DisplayName("un evento que termina publica el cambio a COMPLETED")
    void publishesCompleted() {
        when(eventRepository.findAllPublishedExpiredBefore(any(LocalDate.class))).thenReturn(List.of(Event.builder()
                .eventId(3L).userId(7L).eventName("Concierto").status(EventStatus.PUBLISHED).build()));

        scheduler.completeExpiredEvents();

        ArgumentCaptor<EventStatusChangedEvent> change = ArgumentCaptor.forClass(EventStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(change.capture());
        assertThat(change.getValue().getEventId()).isEqualTo(3L);
        assertThat(change.getValue().getOldStatus()).isEqualTo("PUBLISHED");
        assertThat(change.getValue().getNewStatus()).isEqualTo("COMPLETED");
        assertThat(change.getValue().getOrganizerId()).isEqualTo(7L);
    }
}
