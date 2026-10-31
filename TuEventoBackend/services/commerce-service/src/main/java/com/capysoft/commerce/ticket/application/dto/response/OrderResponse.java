package com.capysoft.commerce.ticket.application.dto.response;

import com.capysoft.commerce.ticket.domain.model.Order;
import com.capysoft.commerce.ticket.domain.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response de orden.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long orderId;
    private Long userId;
    private Long eventId;
    private String eventName;      // nombre del evento (para mostrarlo en "mis entradas")
    private String eventStatus;    // estado del evento: PUBLISHED, CANCELLED, COMPLETED, ...
    private BigDecimal totalAmount;
    private String currency;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private List<TicketResponse> tickets;
    
    public static OrderResponse fromDomain(Order order) {
        return OrderResponse.builder()
            .orderId(order.getOrderId())
            .userId(order.getUserId())
            .eventId(order.getEventId())
            .totalAmount(order.getTotalAmount().getAmount())
            .currency(order.getTotalAmount().getCurrency())
            .status(order.getStatus())
            .createdAt(order.getCreatedAt())
            .build();
    }
    
    public static OrderResponse fromDomainWithTickets(Order order, List<TicketResponse> tickets) {
        return fromDomainWithTickets(order, tickets, null, null);
    }

    public static OrderResponse fromDomainWithTickets(Order order, List<TicketResponse> tickets,
                                                      String eventName, String eventStatus) {
        return OrderResponse.builder()
            .orderId(order.getOrderId())
            .userId(order.getUserId())
            .eventId(order.getEventId())
            .eventName(eventName)
            .eventStatus(eventStatus)
            .totalAmount(order.getTotalAmount().getAmount())
            .currency(order.getTotalAmount().getCurrency())
            .status(order.getStatus())
            .createdAt(order.getCreatedAt())
            .tickets(tickets)
            .build();
    }
}
