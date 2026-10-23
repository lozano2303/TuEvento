package com.capysoft.tuevento.modules.payment.application.usecase;

import com.capysoft.tuevento.modules.payment.application.dto.response.PaymentResponse;
import com.capysoft.tuevento.modules.payment.domain.model.Payment;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentNotFoundException;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case para obtener un pago por ID.
 */
@Service
@RequiredArgsConstructor
public class GetPaymentUseCaseImpl {
    
    private final PaymentRepository paymentRepository;
    
    @Transactional(readOnly = true)
    public PaymentResponse execute(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        
        return PaymentResponse.fromDomain(payment);
    }
}
