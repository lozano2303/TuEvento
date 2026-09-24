package com.capysoft.tuevento.modules.payment.application.usecase;

import com.capysoft.tuevento.modules.payment.application.dto.CreatePaymentCommand;
import com.capysoft.tuevento.modules.payment.application.dto.GatewayPayment;
import com.capysoft.tuevento.modules.payment.application.dto.response.PaymentResponse;
import com.capysoft.tuevento.modules.payment.application.port.out.PaymentGatewayPort;
import com.capysoft.tuevento.modules.payment.domain.model.Payment;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentGateway;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentMethod;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentStatus;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentRepository;
import com.capysoft.tuevento.modules.ticket.domain.model.Money;
import com.capysoft.tuevento.modules.ticket.domain.model.Order;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Subcomponente: cobra un monto concreto por el gateway externo y persiste
 * un Payment en PENDING.
 *
 * NO sabe nada de wallet. Recibe (order, amountToCharge, paymentMethod,
 * walletAmountApplied, walletTransactionId) para que el orquestador
 * pueda pasarle el monto reducido ya calculado.
 *
 * Devuelve PaymentResponse con el Payment persistido.
 * La transición de orden a PAYMENT_PENDING la hace el orquestador antes de llamar aquí.
 */
@Service
@RequiredArgsConstructor
public class ChargeOrderViaGatewayUseCase {

    private static final Logger log = LoggerFactory.getLogger(ChargeOrderViaGatewayUseCase.class);

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayPort paymentGatewayPort;

    @Value("${payment.gateway}")
    private String gatewayName;

    /**
     * @param order                La orden ya en PAYMENT_PENDING
     * @param amountToCharge       Monto que se cobrará por pasarela (puede ser menor al total)
     * @param paymentMethod        Método de pago (ej. "QR")
     * @param walletAmountApplied  Monto ya cubierto por wallet (0 si no aplica)
     * @param walletTransactionId  ID de la reserva wallet (null si no aplica)
     */
    @Transactional
    public PaymentResponse execute(
            Order order,
            BigDecimal amountToCharge,
            String paymentMethod,
            BigDecimal walletAmountApplied,
            Long walletTransactionId) {

        // Crear comando para el gateway con el monto real a cobrar
        CreatePaymentCommand command = CreatePaymentCommand.builder()
            .externalReference(order.getOrderId().toString())
            .amount(amountToCharge)
            .currency(order.getTotalAmount().getCurrency())
            .paymentMethod(paymentMethod != null ? paymentMethod : "QR")
            .build();

        // Crear pago en el gateway externo
        GatewayPayment gatewayPayment = paymentGatewayPort.createPayment(command);

        log.info("Gateway payment created: orderId={}, gatewayTxId={}, amount={}",
            order.getOrderId(), gatewayPayment.getPaymentId(), amountToCharge);

        // Persistir Payment local en PENDING
        Payment payment = Payment.builder()
            .orderId(order.getOrderId())
            .gateway(PaymentGateway.valueOf(gatewayName.toUpperCase()))
            .gatewayTransactionId(gatewayPayment.getPaymentId())
            .status(PaymentStatus.PENDING)
            .amount(new Money(amountToCharge, order.getTotalAmount().getCurrency()))
            .paymentMethod(paymentMethod != null
                ? PaymentMethod.valueOf(paymentMethod.toUpperCase())
                : PaymentMethod.QR)
            .walletAmountApplied(walletAmountApplied != null ? walletAmountApplied : BigDecimal.ZERO)
            .walletTransactionId(walletTransactionId)
            .build();

        Payment savedPayment = paymentRepository.save(payment);
        return PaymentResponse.fromDomain(savedPayment);
    }
}
