package com.capysoft.tuevento.modules.event.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Respuesta del panel de ganancias del organizador.
 * Incluye el total acumulado y el desglose por evento.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizerEarningsResponse {
    private BigDecimal totalEarnings;
    private int totalTicketsSold;
    private String currency;
    private List<OrganizerEarningsByEventResponse> earningsByEvent;
}
