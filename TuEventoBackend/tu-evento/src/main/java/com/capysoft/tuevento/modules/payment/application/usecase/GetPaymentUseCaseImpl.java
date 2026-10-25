package com.capysoft.tuevento.modules.payment.application.usecase;

import com.capysoft.tuevento.modules.payment.application.dto.response.PaymentResponse;
import com.capysoft.tuevento.modules.payment.domain.model.Payment;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentNotFoundException;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentRepository;
import com.capysoft.tuevento.modules.ticket.domain.model.Order;
import com.capysoft.tuevento.modules.ticket.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case para obtener un pago por ID. Solo el comprador de la orden puede verlo.
 */
@Service
@RequiredArgsConstructor
public class GetPaymentUseCaseImpl {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public PaymentResponse execute(Long paymentId, Long requesterId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        boolean owned = orderRepository.findById(payment.getOrderId())
            .map(order -> order.isOwnedBy(requesterId))
            .orElse(false);
        if (!owned) {
            throw new AccessDeniedException("You do not have access to this payment");
        }

        return PaymentResponse.fromDomain(payment);
    }
}
