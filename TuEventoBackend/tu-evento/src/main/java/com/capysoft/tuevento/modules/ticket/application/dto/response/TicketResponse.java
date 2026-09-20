package com.capysoft.tuevento.modules.ticket.application.dto.response;

import com.capysoft.tuevento.modules.ticket.domain.model.Ticket;
import com.capysoft.tuevento.modules.ticket.domain.model.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response de ticket.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {
    private Long ticketId;
    private Long eventId;
    private Long userId;
    private Long orderId;
    private String code;
    private String qrCode;
    private TicketStatus status;
    private LocalDateTime expirationDate;
    private BigDecimal totalPrice;
    private String currency;
    private LocalDateTime createdAt;
    
    public static TicketResponse fromDomain(Ticket ticket) {
        return TicketResponse.builder()
            .ticketId(ticket.getTicketId())
            .eventId(ticket.getEventId())
            .userId(ticket.getUserId())
            .orderId(ticket.getOrderId())
            .code(ticket.getCode())
            .qrCode(ticket.getQrCode())
            .status(ticket.getStatus())
            .expirationDate(ticket.getExpirationDate())
            .totalPrice(ticket.getTotalPrice().getAmount())
            .currency(ticket.getTotalPrice().getCurrency())
            .createdAt(ticket.getCreatedAt())
            .build();
    }
}
