package com.capysoft.tuevento.modules.seat.infrastructure.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.tuevento.modules.seat.domain.event.SeatStatusChangedEvent;
import com.capysoft.tuevento.modules.seat.domain.model.Seat;
import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;
import com.capysoft.tuevento.modules.section.domain.model.EventSection;
import com.capysoft.tuevento.modules.section.domain.repository.EventSectionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler que libera automáticamente las sillas RESERVED cuyo tiempo de reserva
 * (reserved_until) ya expiró.
 * 
 * Se ejecuta en dos momentos:
 *   1. Al arrancar la aplicación (catch-up) — cubre casos donde el backend estuvo
 *      caído y se perdieron ejecuciones programadas.
 *   2. Cada minuto (cron) — caso normal de operación.
 * 
 * Las sillas liberadas pasan de RESERVED a AVAILABLE, con reserved_by y reserved_until
 * limpiados. Se publica un SeatStatusChangedEvent para cada liberación, que será
 * propagado a los clientes WebSocket conectados.
 * 
 * changedBy = null indica cambio automático del sistema (no de un usuario).
 * @EnableScheduling está activado en TuEventoApplication.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatReservationExpirationScheduler {

    private final SeatRepository seatRepository;
    private final EventSectionRepository eventSectionRepository;
    private final ApplicationEventPublisher eventPublisher;

    // Self-injection vía @Lazy para que @Transactional aplique correctamente
    // en la invocación desde onStartup() (evita el problema de self-invocation
    // donde el proxy de Spring no intercepta llamadas internas directas).
    @Lazy
    @Autowired
    private SeatReservationExpirationScheduler self;

    /**
     * Catch-up al arrancar: corre una vez después de que todo el contexto
     * de Spring (DB, beans, transacciones) está completamente inicializado.
     * ApplicationReadyEvent garantiza esto — a diferencia de @PostConstruct
     * o ContextRefreshedEvent que pueden correr antes de que el datasource
     * esté listo.
     * 
     * El log es INFO independientemente de si hay reservas expiradas,
     * para que quede visible en los logs de arranque sin tener que buscar.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("[Scheduler] Running startup catch-up for expired seat reservations...");
        self.releaseExpiredReservations(); // via proxy → @Transactional aplica correctamente
        log.info("[Scheduler] Startup catch-up finished.");
    }

    /**
     * Cron cada minuto — caso normal.
     * @Transactional aplica igual tanto al ser disparado por el cron como
     * desde onStartup(), ya que el proxy de Spring está activo en ambos casos.
     */
    @Scheduled(cron = "0 * * * * *") // cada minuto
    @Transactional
    public void releaseExpiredReservations() {
        List<Seat> expired = seatRepository.findAllByStatusAndReservedUntilBefore(
                SeatStatus.RESERVED, LocalDateTime.now());

        if (expired.isEmpty()) {
            // INFO (no debug) para que sea visible tanto en cron como en catch-up
            log.info("[Scheduler] No expired seat reservations to release.");
            return;
        }

        log.info("[Scheduler] Auto-releasing {} expired seat reservation(s).", expired.size());

        for (Seat seat : expired) {
            SeatStatus oldStatus = seat.getStatus();

            // Construir la silla liberada (Seat es inmutable via Builder)
            Seat released = Seat.builder()
                    .seatId(seat.getSeatId())
                    .seatBlockId(seat.getSeatBlockId())
                    .eventSectionId(seat.getEventSectionId())
                    .code(seat.getCode())
                    .row(seat.getRow())
                    .position(seat.getPosition())
                    .type(seat.getType())
                    .status(SeatStatus.AVAILABLE)
                    .reservedBy(null)
                    .reservedUntil(null)
                    .build();

            seatRepository.save(released);

            // Obtener eventId para el evento de WebSocket
            EventSection section = eventSectionRepository.findById(seat.getEventSectionId())
                    .orElse(null);

            if (section != null) {
                eventPublisher.publishEvent(SeatStatusChangedEvent.builder()
                        .seatId(released.getSeatId())
                        .eventSectionId(released.getEventSectionId())
                        .eventId(section.getEventId())
                        .oldStatus(oldStatus)
                        .newStatus(SeatStatus.AVAILABLE)
                        .changedBy(null) // null = cambio automático del sistema
                        .reservedUntil(null)
                        .build());

                log.debug("[Scheduler] Released seat {} (code: {}) from section {} of event {}",
                        seat.getSeatId(), seat.getCode(), section.getEventSectionId(), section.getEventId());
            } else {
                log.warn("[Scheduler] Released seat {} but could not find EventSection {}",
                        seat.getSeatId(), seat.getEventSectionId());
            }
        }

        log.info("[Scheduler] Finished releasing {} seat reservation(s).", expired.size());
    }
}
