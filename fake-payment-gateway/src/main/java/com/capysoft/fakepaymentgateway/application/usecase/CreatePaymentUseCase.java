package com.capysoft.fakepaymentgateway.application.usecase;

import com.capysoft.fakepaymentgateway.application.dto.request.CreatePaymentRequest;
import com.capysoft.fakepaymentgateway.application.dto.response.PaymentResponse;
import com.capysoft.fakepaymentgateway.domain.model.Money;
import com.capysoft.fakepaymentgateway.domain.model.Payment;
import com.capysoft.fakepaymentgateway.domain.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class CreatePaymentUseCase {
    private final PaymentRepository paymentRepository;
    
    public CreatePaymentUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }
    
    @Transactional
    public PaymentResponse execute(CreatePaymentRequest request) {
        Money money = new Money(
            request.amount(),
            request.currency()
        );
        
        Payment payment = Payment.create(
            request.externalReference(),
            money,
            request.paymentMethod()
        );
        
        Payment savedPayment = paymentRepository.save(payment);
        
        return PaymentResponse.fromDomain(savedPayment);
    }
}
