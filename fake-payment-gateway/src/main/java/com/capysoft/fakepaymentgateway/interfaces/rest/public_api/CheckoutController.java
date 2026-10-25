package com.capysoft.fakepaymentgateway.interfaces.rest.public_api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.capysoft.fakepaymentgateway.application.usecase.ApprovePaymentUseCase;
import com.capysoft.fakepaymentgateway.application.usecase.CancelPaymentUseCase;
import com.capysoft.fakepaymentgateway.application.usecase.DeclinePaymentUseCase;
import com.capysoft.fakepaymentgateway.application.usecase.FailPaymentUseCase;

/**
 * Acciones del pagador desde el checkout hospedado (pay.html).
 *
 * Son públicas como en cualquier pasarela: quien tiene el paymentId (UUID v4,
 * no adivinable) es quien está pagando. El reembolso queda fuera a propósito:
 * es una operación del comercio y solo está disponible en /admin con API key.
 */
@RestController
@RequestMapping("/public/checkout/{paymentId}")
public class CheckoutController {
    private final ApprovePaymentUseCase approvePaymentUseCase;
    private final DeclinePaymentUseCase declinePaymentUseCase;
    private final FailPaymentUseCase failPaymentUseCase;
    private final CancelPaymentUseCase cancelPaymentUseCase;

    public CheckoutController(
        ApprovePaymentUseCase approvePaymentUseCase,
        DeclinePaymentUseCase declinePaymentUseCase,
        FailPaymentUseCase failPaymentUseCase,
        CancelPaymentUseCase cancelPaymentUseCase
    ) {
        this.approvePaymentUseCase = approvePaymentUseCase;
        this.declinePaymentUseCase = declinePaymentUseCase;
        this.failPaymentUseCase = failPaymentUseCase;
        this.cancelPaymentUseCase = cancelPaymentUseCase;
    }

    @PostMapping("/approve")
    public ResponseEntity<Void> approve(@PathVariable String paymentId) {
        approvePaymentUseCase.execute(paymentId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/decline")
    public ResponseEntity<Void> decline(@PathVariable String paymentId) {
        declinePaymentUseCase.execute(paymentId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/fail")
    public ResponseEntity<Void> fail(@PathVariable String paymentId) {
        failPaymentUseCase.execute(paymentId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cancel")
    public ResponseEntity<Void> cancel(@PathVariable String paymentId) {
        cancelPaymentUseCase.execute(paymentId);
        return ResponseEntity.ok().build();
    }
}
