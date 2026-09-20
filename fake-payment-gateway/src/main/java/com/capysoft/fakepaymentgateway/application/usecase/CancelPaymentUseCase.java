package com.capysoft.fakepaymentgateway.application.usecase;

import com.capysoft.fakepaymentgateway.application.port.out.WebhookNotifierPort;
import com.capysoft.fakepaymentgateway.domain.event.PaymentStatusChanged;
import com.capysoft.fakepaymentgateway.domain.model.Payment;
import com.capysoft.fakepaymentgateway.domain.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelPaymentUseCase {
    private final PaymentRepository paymentRepository;
    private final WebhookNotifierPort webhookNotifier;
    
    public CancelPaymentUseCase(
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
        
        PaymentStatusChanged cancelledEvent = payment.cancel();
        paymentRepository.save(payment);
        webhookNotifier.notify(cancelledEvent);
    }
}
