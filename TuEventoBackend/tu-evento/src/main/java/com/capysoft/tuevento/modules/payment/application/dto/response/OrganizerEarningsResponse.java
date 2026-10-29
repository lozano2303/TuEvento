package com.capysoft.tuevento.modules.payment.application.dto.response;

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
    /** Σ ganancia potencial de los eventos no cancelados: precio × sillas que no son de cortesía. */
    private BigDecimal totalPotentialEarnings;
    private long totalCourtesySeats;
    private List<OrganizerEarningsByEventResponse> earningsByEvent;
}
