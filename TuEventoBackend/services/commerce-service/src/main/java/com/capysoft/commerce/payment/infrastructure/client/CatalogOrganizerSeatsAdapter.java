package com.capysoft.commerce.payment.infrastructure.client;

import java.util.List;

import org.springframework.stereotype.Component;

import com.capysoft.commerce.infrastructure.client.CatalogServiceClient;
import com.capysoft.commerce.payment.application.port.out.OrganizerSeatsPort;

import lombok.RequiredArgsConstructor;

/** Resumen de sillas del organizador, leído de la API interna de Catálogo. */
@Component
@RequiredArgsConstructor
class CatalogOrganizerSeatsAdapter implements OrganizerSeatsPort {

    private final CatalogServiceClient catalog;

    @Override
    public List<EventSeats> summarizeByOrganizer(Long organizerId) {
        return catalog.seatSummary(organizerId).stream()
                .map(s -> new EventSeats(s.eventId(), s.eventName(), s.potentialRevenue(), s.courtesySeats(), s.totalSeats()))
                .toList();
    }
}
