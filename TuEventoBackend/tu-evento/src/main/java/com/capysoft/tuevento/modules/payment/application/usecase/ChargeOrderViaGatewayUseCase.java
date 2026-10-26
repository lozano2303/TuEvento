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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Subcomponente: cobra un monto concreto por el gateway externo y persiste
 * un Payment en PENDING.
 *
 * <h3>Garantía transaccional</h3>
 * <p>La llamada HTTP al gateway se realiza FUERA de cualquier transacción activa
 * ({@code propagation = NOT_SUPPORTED}). Esto evita que la conexión a BD permanezca
 * abierta durante la latencia de red y que un rollback posterior deje un pago huérfano
 * en el gateway sin registro local.
 *
 * <p>El flujo es:
 * <ol>
 *   <li>{@code callGateway()} — HTTP externo, sin transacción BD.
 *   <li>{@code persistPayment()} — transacción propia para guardar el Payment en PENDING.
 * </ol>
 *
 * <p>Si {@code persistPayment()} falla después de que el gateway respondió OK, el
 * {@code PaymentReconciliationScheduler} detectará el pago PENDING huérfano en el
 * gateway y lo reconciliará. Si {@code callGateway()} lanza excepción, no se persiste
 * nada y el orquestador maneja el error.
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
     * <p>Este método NO debe estar dentro de una transacción que abarque la llamada HTTP.
     * El orquestador ({@code CreatePaymentUseCaseImpl}) gestiona el rollback de la
     * reserva wallet y el fallo de la orden en caso de excepción.
     *
     * @param order                La orden ya en PAYMENT_PENDING
     * @param amountToCharge       Monto que se cobrará por pasarela
     * @param paymentMethod        Método de pago (ej. "QR")
     * @param walletAmountApplied  Monto ya cubierto por wallet (0 si no aplica)
     * @param walletTransactionId  ID de la reserva wallet (null si no aplica)
     */
    public PaymentResponse execute(
            Order order,
            BigDecimal amountToCharge,
            String paymentMethod,
            BigDecimal walletAmountApplied,
            Long walletTransactionId) {

        // Paso 1: llamada HTTP fuera de cualquier transacción activa
        GatewayPayment gatewayPayment = callGateway(order, amountToCharge, paymentMethod);

        log.info("Gateway payment created: orderId={}, gatewayTxId={}, amount={}",
            order.getOrderId(), gatewayPayment.getPaymentId(), amountToCharge);

        // Paso 2: persistir en transacción propia (no hereda la del orquestador)
        return persistPayment(order, amountToCharge, paymentMethod,
            walletAmountApplied, walletTransactionId, gatewayPayment);
    }

    /**
     * Llama al gateway externo. Se ejecuta con {@code NOT_SUPPORTED} para suspender
     * cualquier transacción activa: la conexión a BD se libera durante la llamada HTTP,
     * evitando timeouts y conexiones idle durante la latencia de red.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public GatewayPayment callGateway(Order order, BigDecimal amount, String paymentMethod) {
        CreatePaymentCommand command = CreatePaymentCommand.builder()
            .externalReference(order.getOrderId().toString())
            .amount(amount)
            .currency(order.getTotalAmount().getCurrency())
            .paymentMethod(paymentMethod != null ? paymentMethod : "QR")
            .build();
        return paymentGatewayPort.createPayment(command);
    }

    /**
     * Persiste el Payment en PENDING en su propia transacción ({@code REQUIRES_NEW}).
     * Si el orquestador hace rollback, este registro persiste igualmente, lo cual es
     * correcto: el pago existe en el gateway y el job de conciliación lo procesará.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentResponse persistPayment(
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
