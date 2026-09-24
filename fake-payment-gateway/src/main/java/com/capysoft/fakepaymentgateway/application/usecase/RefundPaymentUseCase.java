package com.capysoft.fakepaymentgateway.application.usecase;

import com.capysoft.fakepaymentgateway.application.port.out.WebhookNotifierPort;
import com.capysoft.fakepaymentgateway.domain.event.PaymentStatusChanged;
import com.capysoft.fakepaymentgateway.domain.model.Payment;
import com.capysoft.fakepaymentgateway.domain.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso para reembolsar un pago aprobado.
 * Transición válida: APPROVED → REFUNDED.
 * Cualquier otro estado origen lanza InvalidStatusTransitionException.
 */
@Service
public class RefundPaymentUseCase {
    private final PaymentRepository paymentRepository;
    private final WebhookNotifierPort webhookNotifier;

    public RefundPaymentUseCase(
        PaymentRepository paymentRepository,
        WebhookNotifierPort webhookNotifier
    ) {
        this.paymentRepository = paymentRepository;
        this.webhookNotifier = webhookNotifier;
    }

    @Transactional
    public void execute(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
            .orElseThrow(() -> new GetPaymentUseCase.PaymentNotFoundException(paymentId));

        // Dominio valida la transición: solo APPROVED → REFUNDED es válido.
        // Si el estado actual no es APPROVED lanza InvalidStatusTransitionException.
        PaymentStatusChanged refundedEvent = payment.refund();
        paymentRepository.save(payment);
        webhookNotifier.notify(refundedEvent);
    }
}
