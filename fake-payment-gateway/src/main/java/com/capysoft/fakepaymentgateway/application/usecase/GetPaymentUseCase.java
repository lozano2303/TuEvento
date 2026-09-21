package com.capysoft.fakepaymentgateway.application.usecase;

import com.capysoft.fakepaymentgateway.application.dto.response.PaymentResponse;
import com.capysoft.fakepaymentgateway.domain.model.Payment;
import com.capysoft.fakepaymentgateway.domain.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetPaymentUseCase {
    private final PaymentRepository paymentRepository;
    
    public GetPaymentUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }
    
    @Transactional(readOnly = true)
    public PaymentResponse execute(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        
        return PaymentResponse.fromDomain(payment);
    }
    
    public static class PaymentNotFoundException extends RuntimeException {
        public PaymentNotFoundException(String paymentId) {
            super("Payment not found: " + paymentId);
        }
    }
}
