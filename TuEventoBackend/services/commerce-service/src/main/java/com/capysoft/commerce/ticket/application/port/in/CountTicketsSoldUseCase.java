package com.capysoft.commerce.ticket.application.port.in;

import java.util.Collection;
import java.util.Map;

/**
 * Inbound port: tickets sold (paid or already used) per event. Catalog shows it on the event cards
 * and asks for a whole page of events at once.
 */
public interface CountTicketsSoldUseCase {

    /** Sold tickets of each requested event; events without sales map to 0. */
    Map<Long, Long> countSoldByEventIds(Collection<Long> eventIds);
}
