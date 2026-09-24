package com.capysoft.tuevento.modules.payment.application.usecase;

import com.capysoft.tuevento.modules.payment.application.port.out.PaymentGatewayPort;
import com.capysoft.tuevento.modules.payment.domain.model.Payment;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentNotFoundException;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentStatus;
import com.capysoft.tuevento.modules.payment.domain.model.Refund;
import com.capysoft.tuevento.modules.payment.domain.model.RefundStatus;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentRepository;
import com.capysoft.tuevento.modules.payment.domain.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Caso de uso para iniciar un reembolso.
 *
 * Flujo:
 *  1. Carga el Payment y valida que esté en APPROVED.
 *  2. Crea el registro Refund en estado REQUESTED.
 *  3. Llama a PaymentGatewayPort.refundPayment() para disparar la transición
 *     en el fake-gateway, que a su vez dispara el webhook payment.refunded.
 *
 * La actualización final de Payment a REFUNDED y del Refund a PROCESSED ocurre
 * cuando el webhook payment.refunded llega y es procesado por ProcessWebhookUseCaseImpl.
 */
@Service
@RequiredArgsConstructor
public class RequestRefundUseCaseImpl {

    private static final Logger log = LoggerFactory.getLogger(RequestRefundUseCaseImpl.class);

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PaymentGatewayPort paymentGatewayPort;

    @Transactional
    public void execute(Long paymentId, String reason) {
        // 1. Cargar el pago
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        // 2. Validar que el pago esté APPROVED — solo se puede reembolsar un pago aprobado
        if (payment.getStatus() != PaymentStatus.APPROVED) {
            throw new IllegalStateException(
                String.format("Cannot refund payment %d: current status is %s, expected APPROVED",
                    paymentId, payment.getStatus())
            );
        }

        // 3. Crear registro Refund en estado REQUESTED
        Refund refund = Refund.builder()
            .paymentId(paymentId)
            .status(RefundStatus.REQUESTED)
            .reason(reason)
            .requestedAt(LocalDateTime.now())
            .build();
        refundRepository.save(refund);

        log.info("Refund requested: paymentId={}, gatewayTransactionId={}, reason={}",
            paymentId, payment.getGatewayTransactionId(), reason);

        // 4. Notificar al gateway — el gateway cambia el estado a REFUNDED y dispara
        //    el webhook payment.refunded, que será procesado por ProcessWebhookUseCaseImpl
        paymentGatewayPort.refundPayment(payment.getGatewayTransactionId());
    }
}
