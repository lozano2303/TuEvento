package com.capysoft.tuevento.modules.event.application.usecase;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.capysoft.tuevento.modules.event.application.dto.request.ChangeEventStatusRequest;
import com.capysoft.tuevento.modules.event.application.port.in.AdminChangeEventStatusPort;
import com.capysoft.tuevento.modules.event.application.port.in.CloseRevokedOrganizerEventsPort;
import com.capysoft.tuevento.modules.event.domain.event.EventStatusChangedEvent;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cada evento se cierra en su propia transacción ({@link AdminChangeEventStatusPort#execute} es
 * transaccional y aquí no hay una exterior): el fallo de uno no deshace los ya cerrados, y al
 * confirmar cada uno se disparan sus listeners AFTER_COMMIT (reembolsos y aviso en el /ws).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloseRevokedOrganizerEventsUseCase implements CloseRevokedOrganizerEventsPort {

    private final EventRepository            eventRepository;
    private final AdminChangeEventStatusPort adminChangeEventStatusPort;

    @Override
    public int close(Integer organizerUserId, Integer revokedBy, String reason) {
        Long adminId = revokedBy == null ? null : revokedBy.longValue();
        int closed = 0;
        List<Long> failed = new ArrayList<>();

        for (Event event : eventRepository.findByUserId(organizerUserId.longValue())) {
            ChangeEventStatusRequest request = closingRequest(event.getStatus(), reason);
            if (request == null) {
                continue;
            }
            try {
                adminChangeEventStatusPort.execute(event.getEventId(), request, adminId,
                        EventStatusChangedEvent.CAUSE_ORGANIZER_REVOKED);
                closed++;
            } catch (Exception e) {
                log.error("Could not close event {} of revoked organizer {}: {}",
                        event.getEventId(), organizerUserId, e.getMessage());
                failed.add(event.getEventId());
            }
        }

        if (!failed.isEmpty()) {
            throw new IllegalStateException("Could not close events " + failed
                    + " of revoked organizer " + organizerUserId);
        }
        log.info("Closed {} event(s) of revoked organizer {}", closed, organizerUserId);
        return closed;
    }

    /** PUBLISHED → CANCELLED, PENDING_REVIEW → REJECTED; cualquier otro estado no se toca. */
    private static ChangeEventStatusRequest closingRequest(EventStatus status, String reason) {
        if (status == EventStatus.PUBLISHED) {
            return ChangeEventStatusRequest.builder()
                    .newStatus(EventStatus.CANCELLED)
                    .reason("Rol de organizador revocado: " + reason)
                    .build();
        }
        if (status == EventStatus.PENDING_REVIEW) {
            return ChangeEventStatusRequest.builder()
                    .newStatus(EventStatus.REJECTED)
                    .reason("Se revocó tu rol de organizador: " + reason)
                    .build();
        }
        return null;
    }
}
