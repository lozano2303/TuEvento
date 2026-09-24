package com.capysoft.fakepaymentgateway.interfaces.rest.admin;

import com.capysoft.fakepaymentgateway.application.dto.response.PaymentResponse;
import com.capysoft.fakepaymentgateway.application.usecase.ApprovePaymentUseCase;
import com.capysoft.fakepaymentgateway.application.usecase.CancelPaymentUseCase;
import com.capysoft.fakepaymentgateway.application.usecase.DeclinePaymentUseCase;
import com.capysoft.fakepaymentgateway.application.usecase.FailPaymentUseCase;
import com.capysoft.fakepaymentgateway.application.usecase.GetPaymentUseCase;
import com.capysoft.fakepaymentgateway.application.usecase.RefundPaymentUseCase;
import com.capysoft.fakepaymentgateway.domain.repository.PaymentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador administrativo para simular transiciones de estado de pago.
 * También expone GET /admin/payments para listar todos los pagos (útil para el dashboard).
 */
@RestController
@RequestMapping("/admin/payments")
public class AdminPaymentController {
    private final ApprovePaymentUseCase approvePaymentUseCase;
    private final DeclinePaymentUseCase declinePaymentUseCase;
    private final FailPaymentUseCase failPaymentUseCase;
    private final CancelPaymentUseCase cancelPaymentUseCase;
    private final RefundPaymentUseCase refundPaymentUseCase;
    private final GetPaymentUseCase getPaymentUseCase;
    private final PaymentRepository paymentRepository;

    public AdminPaymentController(
        ApprovePaymentUseCase approvePaymentUseCase,
        DeclinePaymentUseCase declinePaymentUseCase,
        FailPaymentUseCase failPaymentUseCase,
        CancelPaymentUseCase cancelPaymentUseCase,
        RefundPaymentUseCase refundPaymentUseCase,
        GetPaymentUseCase getPaymentUseCase,
        PaymentRepository paymentRepository
    ) {
        this.approvePaymentUseCase = approvePaymentUseCase;
        this.declinePaymentUseCase = declinePaymentUseCase;
        this.failPaymentUseCase = failPaymentUseCase;
        this.cancelPaymentUseCase = cancelPaymentUseCase;
        this.refundPaymentUseCase = refundPaymentUseCase;
        this.getPaymentUseCase = getPaymentUseCase;
        this.paymentRepository = paymentRepository;
    }

    /** Lista todos los pagos registrados en el gateway. */
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> listPayments() {
        List<PaymentResponse> payments = paymentRepository.findAll()
                .stream()
                .map(PaymentResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(payments);
    }

    /** Obtiene un pago específico por su gatewayPaymentId. */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String paymentId) {
        return ResponseEntity.ok(getPaymentUseCase.execute(paymentId));
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

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<Void> refundPayment(@PathVariable String paymentId) {
        refundPaymentUseCase.execute(paymentId);
        return ResponseEntity.ok().build();
    }
}
