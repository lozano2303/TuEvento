package com.capysoft.commerce.ticket.interfaces.rest;

import com.capysoft.commerce.ticket.application.dto.request.CreateOrderRequest;
import com.capysoft.commerce.ticket.application.dto.response.OrderResponse;
import com.capysoft.commerce.ticket.application.dto.response.TicketQrResponse;
import com.capysoft.commerce.ticket.application.dto.response.TicketResponse;
import com.capysoft.commerce.ticket.application.usecase.CancelOrderUseCaseImpl;
import com.capysoft.commerce.ticket.application.usecase.CreateOrderWithTicketsUseCaseImpl;
import com.capysoft.commerce.ticket.application.usecase.GenerateOrderQrUseCaseImpl;
import com.capysoft.commerce.ticket.application.usecase.GetMyOrdersUseCaseImpl;
import com.capysoft.commerce.ticket.application.usecase.GetOrderTicketsUseCaseImpl;
import com.capysoft.commerce.ticket.application.usecase.GetOrderUseCaseImpl;
import com.capysoft.commerce.ticket.domain.model.OrderStatus;
import com.capysoft.common.infrastructure.security.SecurityUser;
import com.capysoft.common.interfaces.ApiResponse;
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
    private final GetMyOrdersUseCaseImpl getMyOrdersUseCase;
    private final GenerateOrderQrUseCaseImpl generateOrderQrUseCase;
    
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
     * Listar las órdenes del usuario autenticado (opcionalmente por estado,
     * p. ej. ?status=PAID). Incluye los tickets de cada orden.
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
            @RequestParam(required = false) OrderStatus status,
            @AuthenticationPrincipal SecurityUser principal) {
        List<OrderResponse> orders = getMyOrdersUseCase.execute(
                principal.getUserId().longValue(), status);
        return ResponseEntity.ok(ApiResponse.ok("Orders retrieved successfully", orders));
    }

    /**
     * Generar el QR de la orden completa (solo el comprador, solo en móvil).
     * Al escanearse marca como USED los tickets PAID restantes de la orden.
     */
    @PostMapping("/{orderId:\\d+}/qr")
    public ResponseEntity<ApiResponse<TicketQrResponse>> generateOrderQr(
            @PathVariable Long orderId,
            @AuthenticationPrincipal SecurityUser principal) {
        TicketQrResponse response = generateOrderQrUseCase.execute(
                orderId, principal.getUserId().longValue());
        return ResponseEntity.ok(ApiResponse.ok("Order QR generated", response));
    }

    /**
     * Obtener una orden por ID (solo el comprador).
     */
    @GetMapping("/{orderId:\\d+}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal SecurityUser principal) {
        OrderResponse response = getOrderUseCase.execute(orderId, principal.getUserId().longValue());
        return ResponseEntity.ok(ApiResponse.ok("Order retrieved successfully", response));
    }
    
    /**
     * Obtener los tickets de una orden (solo el comprador).
     */
    @GetMapping("/{orderId:\\d+}/tickets")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getOrderTickets(
            @PathVariable Long orderId,
            @AuthenticationPrincipal SecurityUser principal) {
        List<TicketResponse> tickets = getOrderTicketsUseCase.execute(orderId, principal.getUserId().longValue());
        return ResponseEntity.ok(ApiResponse.ok("Order tickets retrieved successfully", tickets));
    }
    
    /**
     * Cancelar una orden (solo el comprador).
     */
    @PostMapping("/{orderId:\\d+}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal SecurityUser principal) {
        cancelOrderUseCase.execute(orderId, principal.getUserId().longValue());
        return ResponseEntity.ok(ApiResponse.ok("Order cancelled successfully"));
    }
}
