package com.capysoft.tuevento.modules.event.application.port.in;

import com.capysoft.tuevento.modules.event.application.dto.response.AdminEventSummaryResponse;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;

import java.util.List;

/**
 * Port for the admin event listing use case.
 * Returns all events (any status) or filtered by a specific status.
 * Results include organizer name and category label, resolved server-side.
 */
public interface GetAdminEventsPort {

    /**
     * Returns all events when {@code status} is null,
     * or only events matching the given status otherwise.
     */
    List<AdminEventSummaryResponse> getEvents(EventStatus status);
}
