package com.capysoft.tuevento.modules.payment.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Ganancias desglosadas por evento para el panel del organizador.
 *
 * totalRevenue es lo ya cobrado; potentialRevenue es lo que se recaudaría vendiendo todas
 * las sillas que no son de cortesía (Σ precio de la sección × sillas vendibles).
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
    private BigDecimal potentialRevenue;
    private long courtesySeats;
    private long totalSeats;

    /** Constructor de la consulta JPQL de pagos: solo trae lo cobrado; el nombre lo aporta Catálogo. */
    public OrganizerEarningsByEventResponse(Long eventId, int ticketsSold,
                                            BigDecimal totalRevenue, String currency) {
        this(eventId, null, ticketsSold, totalRevenue, currency, BigDecimal.ZERO, 0, 0);
    }
}
