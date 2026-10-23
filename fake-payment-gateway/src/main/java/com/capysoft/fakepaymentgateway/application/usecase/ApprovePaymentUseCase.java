package com.capysoft.fakepaymentgateway.application.usecase;

import com.capysoft.fakepaymentgateway.application.port.out.WebhookNotifierPort;
import com.capysoft.fakepaymentgateway.domain.event.PaymentStatusChanged;
import com.capysoft.fakepaymentgateway.domain.model.Payment;
import com.capysoft.fakepaymentgateway.domain.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovePaymentUseCase {
    private final PaymentRepository paymentRepository;
    private final WebhookNotifierPort webhookNotifier;
    
    public ApprovePaymentUseCase(
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
        
        // Marcar como procesando primero
        PaymentStatusChanged processingEvent = payment.markAsProcessing();
        paymentRepository.save(payment);
        webhookNotifier.notify(processingEvent);
        
        // Luego aprobar
        PaymentStatusChanged approvedEvent = payment.approve();
        paymentRepository.save(payment);
        webhookNotifier.notify(approvedEvent);
    }
}
