package com.capysoft.commerce.payment.application.usecase;

import com.capysoft.commerce.payment.application.dto.CreatePaymentCommand;
import com.capysoft.commerce.payment.application.dto.GatewayPayment;
import com.capysoft.commerce.payment.application.dto.response.PaymentResponse;
import com.capysoft.commerce.payment.application.port.out.PaymentGatewayPort;
import com.capysoft.commerce.payment.domain.model.Payment;
import com.capysoft.commerce.payment.domain.model.PaymentGateway;
import com.capysoft.commerce.payment.domain.model.PaymentMethod;
import com.capysoft.commerce.payment.domain.model.PaymentStatus;
import com.capysoft.commerce.payment.domain.repository.PaymentRepository;
import com.capysoft.commerce.ticket.domain.model.Money;
import com.capysoft.commerce.ticket.domain.model.Order;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Subcomponente: cobra un monto concreto por el gateway externo y persiste
 * un Payment en PENDING.
 *
 * <h3>Garantía transaccional</h3>
 * <p>{@link #execute} se ejecuta con {@code propagation = NOT_SUPPORTED}: si el llamador
 * tiene una transacción abierta, se suspende, así la llamada HTTP a la pasarela nunca
 * retiene una conexión de BD ni queda dentro de un rollback ajeno. Después, el Payment
 * PENDING se guarda en la transacción propia de {@code paymentRepository.save}.
 *
 * <p>Si la pasarela lanza excepción no se persiste nada y el orquestador limpia el estado.
 * Si el guardado falla tras un cobro creado en la pasarela, el pago queda solo en la
 * pasarela: se registra en el log con su {@code gatewayTxId} para conciliarlo a mano.
 *
 * NO sabe nada de wallet. Recibe (order, amountToCharge, paymentMethod,
 * walletAmountApplied, walletTransactionId) para que el orquestador
 * pueda pasarle el monto reducido ya calculado.
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
     * Coordina la llamada al gateway y la persistencia del resultado.
     *
     * <p>El orquestador ({@code CreatePaymentUseCaseImpl}) libera la reserva wallet y
     * falla la orden si este método lanza excepción.
     *
     * @param order                La orden ya en PAYMENT_PENDING
     * @param amountToCharge       Monto que se cobrará por pasarela
     * @param paymentMethod        Método de pago (ej. "QR")
     * @param walletAmountApplied  Monto ya cubierto por wallet (0 si no aplica)
     * @param walletTransactionId  ID de la reserva wallet (null si no aplica)
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PaymentResponse execute(
            Order order,
            BigDecimal amountToCharge,
            String paymentMethod,
            BigDecimal walletAmountApplied,
            Long walletTransactionId) {

        // Paso 1: llamada HTTP, sin transacción abierta
        GatewayPayment gatewayPayment = callGateway(order, amountToCharge, paymentMethod);

        log.info("Gateway payment created: orderId={}, gatewayTxId={}, amount={}",
            order.getOrderId(), gatewayPayment.getPaymentId(), amountToCharge);

        // Paso 2: persistir el Payment PENDING (save abre su propia transacción)
        try {
            return persistPayment(order, amountToCharge, paymentMethod,
                walletAmountApplied, walletTransactionId, gatewayPayment);
        } catch (RuntimeException e) {
            log.error("Gateway payment created but not saved locally: orderId={}, gatewayTxId={}",
                order.getOrderId(), gatewayPayment.getPaymentId(), e);
            throw e;
        }
    }

    private GatewayPayment callGateway(Order order, BigDecimal amount, String paymentMethod) {
        CreatePaymentCommand command = CreatePaymentCommand.builder()
            .externalReference(order.getOrderId().toString())
            .amount(amount)
            .currency(order.getTotalAmount().getCurrency())
            .paymentMethod(paymentMethod != null ? paymentMethod : "QR")
            .build();
        return paymentGatewayPort.createPayment(command);
    }

    private PaymentResponse persistPayment(
            Order order,
            BigDecimal amountToCharge,
            String paymentMethod,
            BigDecimal walletAmountApplied,
            Long walletTransactionId,
            GatewayPayment gatewayPayment) {

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
