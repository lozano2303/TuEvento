package com.capysoft.tuevento.modules.ticket.application.dto.response;

import com.capysoft.tuevento.modules.ticket.domain.model.Order;
import com.capysoft.tuevento.modules.ticket.domain.model.OrderStatus;
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
        OrderResponse response = fromDomain(order);
        return OrderResponse.builder()
            .orderId(response.getOrderId())
            .userId(response.getUserId())
            .eventId(response.getEventId())
            .totalAmount(response.getTotalAmount())
            .currency(response.getCurrency())
            .status(response.getStatus())
            .createdAt(response.getCreatedAt())
            .tickets(tickets)
            .build();
    }
}
