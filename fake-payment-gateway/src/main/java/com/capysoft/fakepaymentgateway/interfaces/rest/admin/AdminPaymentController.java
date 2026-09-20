package com.capysoft.fakepaymentgateway.interfaces.rest.admin;

import com.capysoft.fakepaymentgateway.application.usecase.ApprovePaymentUseCase;
import com.capysoft.fakepaymentgateway.application.usecase.CancelPaymentUseCase;
import com.capysoft.fakepaymentgateway.application.usecase.DeclinePaymentUseCase;
import com.capysoft.fakepaymentgateway.application.usecase.FailPaymentUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador administrativo para simular transiciones de estado de pago.
 */
@RestController
@RequestMapping("/admin/payments")
public class AdminPaymentController {
    private final ApprovePaymentUseCase approvePaymentUseCase;
    private final DeclinePaymentUseCase declinePaymentUseCase;
    private final FailPaymentUseCase failPaymentUseCase;
    private final CancelPaymentUseCase cancelPaymentUseCase;
    
    public AdminPaymentController(
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
    
    @PostMapping("/{paymentId}/approve")
    public ResponseEntity<Void> approvePayment(@PathVariable String paymentId) {
        approvePaymentUseCase.execute(paymentId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{paymentId}/decline")
    public ResponseEntity<Void> declinePayment(@PathVariable String paymentId) {
        declinePaymentUseCase.execute(paymentId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{paymentId}/fail")
    public ResponseEntity<Void> failPayment(@PathVariable String paymentId) {
        failPaymentUseCase.execute(paymentId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<Void> cancelPayment(@PathVariable String paymentId) {
        cancelPaymentUseCase.execute(paymentId);
        return ResponseEntity.ok().build();
    }
}
