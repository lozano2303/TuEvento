package com.capysoft.tuevento.modules.event.infrastructure.scheduler;

import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.model.EventStatusLog;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventStatusLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler diario que marca como COMPLETED todos los eventos PUBLISHED
 * cuya fecha de finalización ya pasó. Corre a las 00:05 cada día.
 *
 * changedBy se guarda como null para indicar que el cambio fue automático
 * (sistema), no iniciado por un usuario.
 *
 * @EnableScheduling está activado en TuEventoApplication.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventAutoCompletionScheduler {

    private final EventRepository          eventRepository;
    private final EventStatusLogRepository eventStatusLogRepository;

    @Scheduled(cron = "0 5 0 * * *") // todos los días a las 00:05
    @Transactional
    public void completeExpiredEvents() {
        List<Event> expired = eventRepository.findAllPublishedExpiredBefore(LocalDate.now());

        if (expired.isEmpty()) {
            log.debug("[Scheduler] No expired PUBLISHED events to complete.");
            return;
        }

        log.info("[Scheduler] Auto-completing {} expired event(s).", expired.size());

        for (Event event : expired) {
            EventStatus oldStatus = event.getStatus();

            // Reconstruir el evento con el nuevo estado (Event es inmutable via Builder)
            eventRepository.save(Event.builder()
                    .eventId(event.getEventId())
                    .userId(event.getUserId())
                    .siteId(event.getSiteId())
                    .eventName(event.getEventName())
                    .description(event.getDescription())
                    .startDate(event.getStartDate())
                    .finishDate(event.getFinishDate())
                    .status(EventStatus.COMPLETED)
                    .isPublic(event.getIsPublic())
                    .availableSeats(event.getAvailableSeats())
                    .build());

            eventStatusLogRepository.save(EventStatusLog.builder()
                    .eventId(event.getEventId())
                    .oldStatus(oldStatus)
                    .newStatus(EventStatus.COMPLETED)
                    .changedAt(LocalDateTime.now())
                    .changedBy(null) // null = cambio automático del sistema
                    .build());

            log.info("[Scheduler] Event {} '{}' marked as COMPLETED (was {}).",
                    event.getEventId(), event.getEventName(), oldStatus);
        }
    }
}
