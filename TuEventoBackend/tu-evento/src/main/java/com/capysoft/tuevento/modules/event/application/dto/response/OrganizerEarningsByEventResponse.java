package com.capysoft.tuevento.modules.event.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Ganancias desglosadas por evento para el panel del organizador.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizerEarningsByEventResponse {
    private Long eventId;
    private String eventName;
    private int ticketsSold;
    private BigDecimal totalRevenue;
    private String currency;
}
