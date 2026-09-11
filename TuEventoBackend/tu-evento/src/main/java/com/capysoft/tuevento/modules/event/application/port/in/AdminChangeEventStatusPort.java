package com.capysoft.tuevento.modules.event.application.port.in;

import com.capysoft.tuevento.modules.event.application.dto.request.ChangeEventStatusRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventStatusLogResponse;

/**
 * Admin variant of event status change — bypasses ownership enforcement
 * so an admin can act on any event regardless of who created it.
 * Business-logic validations (media required for publish, date check for
 * complete) are still applied.
 */
public interface AdminChangeEventStatusPort {

    /** adminUserId is the ID of the ADMIN executing the action (used for audit log). */
    EventStatusLogResponse execute(Long eventId, ChangeEventStatusRequest request, Long adminUserId);
}
