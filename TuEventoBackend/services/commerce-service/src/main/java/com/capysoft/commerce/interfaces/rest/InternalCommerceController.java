package com.capysoft.commerce.interfaces.rest;

import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.capysoft.commerce.payment.application.port.in.GetCommerceAdminStatsPort;
import com.capysoft.commerce.payment.application.port.in.GetCommerceAdminStatsPort.CommerceStats;
import com.capysoft.commerce.payment.application.port.in.GetCommerceAdminStatsPort.RecentPayment;
import com.capysoft.commerce.ticket.application.port.in.FindSeatsWithPendingPaymentUseCase;
import com.capysoft.common.interfaces.ApiResponse;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;

/**
 * API interna de Comercio. No la usan web ni móvil y el gateway no la enruta; exige la clave entre
 * servicios ({@code InternalApiKeyFilter}).
 */
@Hidden
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalCommerceController {

    private final FindSeatsWithPendingPaymentUseCase pendingPaymentSeats;
    private final GetCommerceAdminStatsPort adminStats;

    /** Sillas con la orden en PAYMENT_PENDING: el planificador de Catálogo no debe liberarlas. */
    @PostMapping("/seats/pending-payment")
    public ResponseEntity<ApiResponse<Set<Integer>>> seatsWithPendingPayment(@RequestBody List<Integer> seatIds) {
        return ResponseEntity.ok(ApiResponse.ok("Seats with pending payment",
                pendingPaymentSeats.findSeatIdsWithPendingPayment(seatIds)));
    }

    @GetMapping("/admin/stats")
    public ResponseEntity<ApiResponse<CommerceStats>> stats() {
        return ResponseEntity.ok(ApiResponse.ok("Commerce stats", adminStats.getStats()));
    }

    @GetMapping("/admin/recent-payments")
    public ResponseEntity<ApiResponse<List<RecentPayment>>> recentPayments(@RequestParam int limit) {
        return ResponseEntity.ok(ApiResponse.ok("Recent payments", adminStats.findRecentPayments(limit)));
    }
}
