package com.capysoft.tuevento.modules.event.infrastructure.scheduler;

import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.model.EventStatusLog;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventStatusLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler que marca como COMPLETED todos los eventos PUBLISHED cuya
 * fecha de finalización ya pasó.
 *
 * Se ejecuta en dos momentos:
 *   1. Al arrancar la aplicación (catch-up) — cubre reinicios donde se
 *      perdió la ejecución programada de medianoche.
 *   2. Diariamente a las 00:05 (cron) — caso normal.
 *
 * changedBy = null indica cambio automático del sistema (no de un usuario).
 * @EnableScheduling está activado en TuEventoApplication.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventAutoCompletionScheduler {

    private final EventRepository          eventRepository;
    private final EventStatusLogRepository eventStatusLogRepository;

    // Self-injection vía @Lazy para que @Transactional aplique correctamente
    // en la invocación desde onStartup() (evita el problema de self-invocation
    // donde el proxy de Spring no intercepta llamadas internas directas).
    @Lazy
    @Autowired
    private EventAutoCompletionScheduler self;

    /**
     * Catch-up al arrancar: corre una vez después de que todo el contexto
     * de Spring (DB, beans, transacciones) está completamente inicializado.
     * ApplicationReadyEvent garantiza esto — a diferencia de @PostConstruct
     * o ContextRefreshedEvent que pueden correr antes de que el datasource
     * esté listo.
     *
     * El log es INFO independientemente de si hay eventos expirados,
     * para que quede visible en los logs de arranque sin tener que buscar.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("[Scheduler] Running startup catch-up for expired events...");
        self.completeExpiredEvents(); // via proxy → @Transactional aplica correctamente
        log.info("[Scheduler] Startup catch-up finished.");
    }

    /**
     * Cron diario a las 00:05 — caso normal.
     * @Transactional aplica igual tanto al ser disparado por el cron como
     * desde onStartup(), ya que el proxy de Spring está activo en ambos casos.
     */
    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void completeExpiredEvents() {
        List<Event> expired = eventRepository.findAllPublishedExpiredBefore(LocalDate.now());

        if (expired.isEmpty()) {
            // INFO (no debug) para que sea visible tanto en cron como en catch-up
            log.info("[Scheduler] No expired PUBLISHED events to complete.");
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
