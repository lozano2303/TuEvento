package com.capysoft.tuevento.modules.ticket.interfaces.rest;

import com.capysoft.tuevento.modules.ticket.application.dto.request.CreateOrderRequest;
import com.capysoft.tuevento.modules.ticket.application.dto.response.OrderResponse;
import com.capysoft.tuevento.modules.ticket.application.dto.response.TicketResponse;
import com.capysoft.tuevento.modules.ticket.application.usecase.CancelOrderUseCaseImpl;
import com.capysoft.tuevento.modules.ticket.application.usecase.CreateOrderWithTicketsUseCaseImpl;
import com.capysoft.tuevento.modules.ticket.application.usecase.GetOrderTicketsUseCaseImpl;
import com.capysoft.tuevento.modules.ticket.application.usecase.GetOrderUseCaseImpl;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityUser;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestión de órdenes.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final CreateOrderWithTicketsUseCaseImpl createOrderWithTicketsUseCase;
    private final GetOrderUseCaseImpl getOrderUseCase;
    private final GetOrderTicketsUseCaseImpl getOrderTicketsUseCase;
    private final CancelOrderUseCaseImpl cancelOrderUseCase;
    
    /**
     * Crear una orden con tickets desde sillas seleccionadas.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal SecurityUser securityUser) {
        
        Long userId = securityUser.getUserId().longValue();
        OrderResponse response = createOrderWithTicketsUseCase.execute(request, userId);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Order created successfully", response));
    }
    
    /**
     * Obtener una orden por ID.
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable Long orderId) {
        OrderResponse response = getOrderUseCase.execute(orderId);
        return ResponseEntity.ok(ApiResponse.ok("Order retrieved successfully", response));
    }
    
    /**
     * Obtener los tickets de una orden.
     */
    @GetMapping("/{orderId}/tickets")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getOrderTickets(@PathVariable Long orderId) {
        List<TicketResponse> tickets = getOrderTicketsUseCase.execute(orderId);
        return ResponseEntity.ok(ApiResponse.ok("Order tickets retrieved successfully", tickets));
    }
    
    /**
     * Cancelar una orden.
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable Long orderId) {
        cancelOrderUseCase.execute(orderId);
        return ResponseEntity.ok(ApiResponse.ok("Order cancelled successfully"));
    }
}
