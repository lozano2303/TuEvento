package com.capysoft.tuevento.modules.ticket.application.dto.response;

import com.capysoft.tuevento.modules.ticket.domain.model.TicketCheckin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response de check-in de ticket.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketCheckinResponse {
    private Long checkinId;
    private Long ticketId;
    private LocalDateTime checkinTime;
    private Long validatedBy;
    
    public static TicketCheckinResponse fromDomain(TicketCheckin checkin) {
        return TicketCheckinResponse.builder()
            .checkinId(checkin.getCheckinId())
            .ticketId(checkin.getTicketId())
            .checkinTime(checkin.getCheckinTime())
            .validatedBy(checkin.getValidatedBy())
            .build();
    }
}
