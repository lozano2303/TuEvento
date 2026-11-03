package com.capysoft.tuevento.modules.event.infrastructure.client;

import java.util.Collection;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.catalogbridge.client.CommerceServiceClient;
import com.capysoft.tuevento.modules.event.application.port.out.TicketSalesPort;

import lombok.RequiredArgsConstructor;

/** Tickets sold per event, from commerce-service. */
@Component
@RequiredArgsConstructor
class CommerceTicketSalesAdapter implements TicketSalesPort {

    private final CommerceServiceClient commerceServiceClient;

    @Override
    public Map<Long, Long> countSoldByEventIds(Collection<Long> eventIds) {
        return commerceServiceClient.ticketsSold(eventIds);
    }
}
