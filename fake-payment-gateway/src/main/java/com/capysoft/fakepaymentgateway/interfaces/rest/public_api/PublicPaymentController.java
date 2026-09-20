package com.capysoft.fakepaymentgateway.interfaces.rest.public_api;

import com.capysoft.fakepaymentgateway.application.dto.request.CreatePaymentRequest;
import com.capysoft.fakepaymentgateway.application.dto.response.PaymentResponse;
import com.capysoft.fakepaymentgateway.application.usecase.CreatePaymentUseCase;
import com.capysoft.fakepaymentgateway.application.usecase.GetPaymentUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador público para operaciones de pago disponibles externamente.
 */
@RestController
@RequestMapping("/public/payments")
public class PublicPaymentController {
    private final CreatePaymentUseCase createPaymentUseCase;
    private final GetPaymentUseCase getPaymentUseCase;
    
    public PublicPaymentController(
        CreatePaymentUseCase createPaymentUseCase,
        GetPaymentUseCase getPaymentUseCase
    ) {
        this.createPaymentUseCase = createPaymentUseCase;
        this.getPaymentUseCase = getPaymentUseCase;
    }
    
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        PaymentResponse response = createPaymentUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String paymentId) {
        PaymentResponse response = getPaymentUseCase.execute(paymentId);
        return ResponseEntity.ok(response);
    }
}
