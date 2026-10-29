package com.capysoft.tuevento.modules.payment.infrastructure.client;

import java.util.List;

import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.payment.application.port.out.OrganizerSeatsPort;
import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;

import lombok.RequiredArgsConstructor;

/**
 * Único punto de payment que conoce a seat para el panel de ganancias. Hoy delega en el
 * repositorio del monolito; al separar Catálogo se sustituye por un cliente de su API interna.
 */
@Component
@RequiredArgsConstructor
class CatalogOrganizerSeatsAdapter implements OrganizerSeatsPort {

    private final SeatRepository seatRepository;

    @Override
    public List<EventSeats> summarizeByOrganizer(Long organizerId) {
        return seatRepository.summarizeSeatsByOrganizer(organizerId).stream()
                .map(s -> new EventSeats(s.eventId(), s.eventName(), s.potentialRevenue(),
                        s.courtesySeats(), s.totalSeats()))
                .toList();
    }
}
