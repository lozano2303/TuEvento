package com.capysoft.tuevento.modules.payment.interfaces.rest;

import com.capysoft.tuevento.modules.payment.application.dto.request.CreatePaymentRequest;
import com.capysoft.tuevento.modules.payment.application.dto.request.RefundPaymentRequest;
import com.capysoft.tuevento.modules.payment.application.dto.response.PaymentResponse;
import com.capysoft.tuevento.modules.payment.application.usecase.CreatePaymentUseCaseImpl;
import com.capysoft.tuevento.modules.payment.application.usecase.GetPaymentUseCaseImpl;
import com.capysoft.tuevento.modules.payment.application.usecase.RequestRefundUseCaseImpl;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para pagos.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    
    private final CreatePaymentUseCaseImpl createPaymentUseCase;
    private final GetPaymentUseCaseImpl getPaymentUseCase;
    private final RequestRefundUseCaseImpl requestRefundUseCase;
    
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        PaymentResponse response = createPaymentUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long id) {
        PaymentResponse response = getPaymentUseCase.execute(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Solicita el reembolso de un pago aprobado.
     * Accesible solo por ADMIN u ORGANIZER.
     */
    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ORGANIZER')")
    public ResponseEntity<ApiResponse<Void>> refundPayment(
            @PathVariable Long id,
            @Valid @RequestBody RefundPaymentRequest request) {
        requestRefundUseCase.execute(id, request.reason());
        return ResponseEntity.ok(ApiResponse.ok("Refund requested successfully"));
    }
}
