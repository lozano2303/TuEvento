package com.capysoft.tuevento.modules.payment.interfaces.rest;

import com.capysoft.tuevento.modules.payment.application.dto.request.CreatePaymentRequest;
import com.capysoft.tuevento.modules.payment.application.dto.response.PaymentResponse;
import com.capysoft.tuevento.modules.payment.application.usecase.CreatePaymentUseCaseImpl;
import com.capysoft.tuevento.modules.payment.application.usecase.GetPaymentUseCaseImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
}
