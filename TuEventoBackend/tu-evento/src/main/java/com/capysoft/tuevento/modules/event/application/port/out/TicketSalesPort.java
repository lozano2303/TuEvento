package com.capysoft.tuevento.modules.event.application.port.out;

import java.util.Collection;
import java.util.Map;

/** Outbound port: tickets sold per event, owned by Commerce. */
public interface TicketSalesPort {

    /** Sold tickets (paid or used) of each event; events without sales may be missing. */
    Map<Long, Long> countSoldByEventIds(Collection<Long> eventIds);
}
