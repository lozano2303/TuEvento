package com.capysoft.commerce.ticket.application.usecase;

import com.capysoft.commerce.ticket.application.port.out.EventLookupPort;
import com.capysoft.commerce.ticket.application.port.out.EventLookupPort.EventRef;
import com.capysoft.commerce.ticket.application.dto.response.ScanResultResponse;
import com.capysoft.commerce.ticket.application.dto.response.ScanResultResponse.ScannedTicket;
import com.capysoft.commerce.ticket.domain.event.TicketsValidatedEvent;
import com.capysoft.commerce.ticket.domain.model.Ticket;
import com.capysoft.commerce.ticket.domain.model.TicketLog;
import com.capysoft.commerce.ticket.domain.model.TicketStatus;
import com.capysoft.commerce.ticket.domain.model.TicketCheckin;
import com.capysoft.commerce.ticket.domain.port.QrNonceStore;
import com.capysoft.commerce.ticket.domain.port.QrTokenPort;
import com.capysoft.commerce.ticket.domain.port.QrTokenPort.QrPayload;
import com.capysoft.commerce.ticket.domain.port.QrTokenPort.QrScope;
import com.capysoft.commerce.ticket.domain.repository.TicketCheckinRepository;
import com.capysoft.commerce.ticket.domain.repository.TicketLogRepository;
import com.capysoft.commerce.ticket.domain.repository.TicketRepository;
import com.capysoft.common.domain.exception.BusinessException;
import com.capysoft.common.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Valida un token QR escaneado por un organizador y marca el/los ticket(s) como USED.
 *
 * Flujo:
 *   1. Verifica firma + expiración del JWT (fuente de verdad de la caducidad).
 *   2. Consume el nonce de forma atómica: si no es el vigente → no se valida nada.
 *      Si sus entradas ya estaban usadas (el mismo QR escaneado otra vez) se responde
 *      "ya usada" para que el organizador vea "Entrada ya escaneada"; si no (QR
 *      regenerado) → rechazo. Esto impide reutilizar/revender un QR.
 *   3. Autoriza que el organizador autenticado sea el DUEÑO del evento del QR
 *      (ningún otro organizador puede escanear) → 403 si no.
 *   4. Marca como USED el ticket (scope TICKET) o todos los tickets PAID
 *      restantes de la orden (scope ORDER). Los ya usados se reportan aparte.
 *   5. Avisa a cada dueño de los tickets validados (tras el commit) para que su app
 *      deje de mostrar el QR.
 */
@Service
@RequiredArgsConstructor
public class ScanQrUseCaseImpl {

    private final QrTokenPort qrTokenPort;
    private final QrNonceStore qrNonceStore;
    private final TicketRepository ticketRepository;
    private final TicketCheckinRepository ticketCheckinRepository;
    private final TicketLogRepository ticketLogRepository;
    private final EventLookupPort eventLookupPort;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ScanResultResponse execute(String qrToken, Long organizerUserId) {
        // 1. Verificar firma + expiración
        QrPayload payload = qrTokenPort.parse(qrToken)
                .orElseThrow(() -> new BusinessException("QR_INVALID",
                        "QR code is invalid or has expired"));

        // 2. Consumir nonce (single-use + anti-regeneración). Fail-closed.
        boolean consumed = qrNonceStore.consume(payload.scope(), payload.targetId(), payload.nonce());
        if (!consumed) {
            return alreadyUsedResult(payload, organizerUserId)
                    .orElseThrow(() -> new BusinessException("QR_EXPIRED_OR_REPLACED",
                            "QR code is no longer valid. Ask the attendee to generate a new one."));
        }

        // 3. Autorizar: el organizador debe ser dueño del evento
        authorizeOrganizer(payload, organizerUserId);

        // 4. Resolver los tickets a validar según el alcance
        List<Ticket> targetTickets = switch (payload.scope()) {
            case TICKET -> {
                // Lectura con bloqueo: si hay un reembolso en curso, espera y ve su resultado
                Ticket t = ticketRepository.findByIdForUpdate(payload.targetId())
                        .orElseThrow(() -> new NotFoundException("TICKET_NOT_FOUND",
                                "Ticket not found for this QR"));
                yield List.of(t);
            }
            case ORDER -> ticketRepository.findByOrderIdForUpdate(payload.targetId());
        };

        if (targetTickets.isEmpty()) {
            throw new BusinessException("QR_NO_TICKETS", "No tickets associated with this QR");
        }

        List<ScannedTicket> validated = new ArrayList<>();
        List<ScannedTicket> alreadyUsed = new ArrayList<>();
        Map<Long, List<Long>> validatedByOwner = new LinkedHashMap<>();

        for (Ticket ticket : targetTickets) {
            // Defensa: todos los tickets deben pertenecer al mismo evento del QR
            if (!ticket.getEventId().equals(payload.eventId())) {
                continue;
            }

            if (ticket.getStatus() == TicketStatus.USED
                    || ticketCheckinRepository.existsByTicketId(ticket.getTicketId())) {
                alreadyUsed.add(toScanned(ticket));
                continue;
            }

            // Para un ticket individual exigimos que sea validable (PAID + no expirado).
            // Para una orden, saltamos silenciosamente los no-PAID (cancelados/reembolsados).
            if (!ticket.canCheckin()) {
                if (payload.scope() == QrScope.TICKET) {
                    throw new BusinessException("TICKET_NOT_VALIDATABLE",
                            "Ticket cannot be validated (status: " + ticket.getStatus() + ")");
                }
                continue;
            }

            markUsed(ticket, organizerUserId);
            validated.add(toScanned(ticket));
            validatedByOwner.computeIfAbsent(ticket.getUserId(), k -> new ArrayList<>()).add(ticket.getTicketId());
        }

        if (validated.isEmpty() && alreadyUsed.isEmpty()) {
            throw new BusinessException("QR_NO_VALIDATABLE_TICKETS",
                    "No tickets could be validated with this QR");
        }

        validatedByOwner.forEach((userId, ticketIds) -> eventPublisher.publishEvent(new TicketsValidatedEvent(
                userId, payload.scope().name(), payload.targetId(), payload.eventId(), ticketIds)));

        return ScanResultResponse.builder()
                .scope(payload.scope().name())
                .eventId(payload.eventId())
                .orderId(payload.scope() == QrScope.ORDER ? payload.targetId() : null)
                .validatedTickets(validated)
                .alreadyUsedTickets(alreadyUsed)
                .validatedCount(validated.size())
                .alreadyUsedCount(alreadyUsed.size())
                .build();
    }

    private void authorizeOrganizer(QrPayload payload, Long organizerUserId) {
        EventRef event = eventLookupPort.find(payload.eventId())
                .orElseThrow(() -> new NotFoundException("EVENT_NOT_FOUND",
                        "Event not found for this QR"));
        if (!event.ownerId().equals(organizerUserId)) {
            throw new AccessDeniedException("You are not the organizer of this event");
        }
    }

    /**
     * QR cuyo nonce ya no es el vigente: si todas sus entradas ya estaban usadas es el mismo
     * QR escaneado otra vez y se responde "ya usada" (sin validar nada ni avisar a nadie).
     * Vacío si queda alguna entrada sin usar: el QR fue regenerado y debe rechazarse.
     * Solo el organizador dueño del evento puede ver el estado de las entradas.
     */
    private Optional<ScanResultResponse> alreadyUsedResult(QrPayload payload, Long organizerUserId) {
        authorizeOrganizer(payload, organizerUserId);

        List<Ticket> tickets = switch (payload.scope()) {
            case TICKET -> ticketRepository.findById(payload.targetId()).map(List::of).orElse(List.of());
            case ORDER -> ticketRepository.findByOrderId(payload.targetId());
        };
        List<ScannedTicket> used = new ArrayList<>();
        for (Ticket ticket : tickets) {
            if (!ticket.getEventId().equals(payload.eventId())) {
                continue;
            }
            if (ticket.getStatus() == TicketStatus.USED) {
                used.add(toScanned(ticket));
            } else if (ticket.canCheckin()) {
                return Optional.empty();
            }
        }
        if (used.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ScanResultResponse.builder()
                .scope(payload.scope().name())
                .eventId(payload.eventId())
                .orderId(payload.scope() == QrScope.ORDER ? payload.targetId() : null)
                .validatedTickets(List.of())
                .alreadyUsedTickets(used)
                .validatedCount(0)
                .alreadyUsedCount(used.size())
                .build());
    }

    private void markUsed(Ticket ticket, Long organizerUserId) {
        TicketStatus oldStatus = ticket.getStatus();

        TicketCheckin checkin = TicketCheckin.builder()
                .ticketId(ticket.getTicketId())
                .checkinTime(LocalDateTime.now())
                .validatedBy(organizerUserId)
                .build();
        ticketCheckinRepository.save(checkin);

        ticket.markAsUsed();
        ticketRepository.save(ticket);

        TicketLog log = TicketLog.builder()
                .ticketId(ticket.getTicketId())
                .oldStatus(oldStatus)
                .newStatus(TicketStatus.USED)
                .changedAt(LocalDateTime.now())
                .changedBy(String.valueOf(organizerUserId))
                .reason("QR scan check-in")
                .build();
        ticketLogRepository.save(log);
    }

    private ScannedTicket toScanned(Ticket ticket) {
        return ScannedTicket.builder()
                .ticketId(ticket.getTicketId())
                .code(ticket.getCode())
                .build();
    }
}
