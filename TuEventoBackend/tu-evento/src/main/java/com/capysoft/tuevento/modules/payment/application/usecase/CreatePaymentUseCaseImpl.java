package com.capysoft.tuevento.modules.payment.application.usecase;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.capysoft.tuevento.modules.payment.application.dto.request.CreatePaymentRequest;
import com.capysoft.tuevento.modules.payment.application.dto.response.PaymentResponse;
import com.capysoft.tuevento.modules.payment.domain.model.Payment;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentGateway;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentGatewayException;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentMethod;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentStatus;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentRepository;
import com.capysoft.tuevento.modules.ticket.application.port.in.FailOrderPaymentUseCase;
import com.capysoft.tuevento.modules.ticket.application.port.in.InitiateOrderPaymentUseCase;
import com.capysoft.tuevento.modules.ticket.domain.model.Money;
import com.capysoft.tuevento.modules.ticket.domain.model.Order;
import com.capysoft.tuevento.modules.ticket.domain.model.OrderNotFoundException;
import com.capysoft.tuevento.modules.ticket.domain.repository.OrderRepository;
import com.capysoft.tuevento.modules.wallet.application.dto.WalletResponse;
import com.capysoft.tuevento.modules.wallet.application.usecase.GetWalletUseCaseImpl;
import com.capysoft.tuevento.modules.wallet.application.usecase.ReleaseWalletPaymentUseCaseImpl;
import com.capysoft.tuevento.modules.wallet.application.usecase.ReserveWalletPaymentUseCaseImpl;
import com.capysoft.tuevento.modules.wallet.domain.model.WalletNotFoundException;

import lombok.RequiredArgsConstructor;

/**
 * ORQUESTADOR de creación de pagos.
 *
 * <h3>Separación transaccional</h3>
 * <p>El método {@code execute()} NO tiene {@code @Transactional} porque incluye
 * una llamada HTTP al gateway externo. Tener una transacción de BD abierta durante
 * esa llamada genera dos problemas:
 * <ol>
 *   <li>La conexión permanece ociosa durante toda la latencia de red (agota el pool).
 *   <li>Si el gateway responde OK pero algo falla después, el rollback deja un pago
 *       huérfano en el gateway sin registro local.
 * </ol>
 *
 * <p>En su lugar cada paso confirma en la transacción de su propio bean:
 * <ul>
 *   <li>{@code prepareOrder()} — verifica la propiedad y pasa la orden a PAYMENT_PENDING
 *       ({@code InitiateOrderPaymentUseCase} confirma antes de llamar a la pasarela).
 *   <li>{@code chargeOrderViaGatewayUseCase.execute()} — llama a la pasarela sin
 *       transacción ({@code NOT_SUPPORTED}) y guarda el Payment PENDING.
 *   <li>{@code handleGatewayFailure()} — limpieza best-effort; cada paso confirma por su
 *       cuenta para que el fallo de uno no deshaga los demás.
 * </ul>
 *
 * <h3>Flujos</h3>
 * <p>Flujo A — sin wallet: llama al gateway con el total.
 * <p>Flujo B — saldo parcial: reserva wallet → gateway con el remainder.
 * <p>Flujo C — saldo suficiente: pago 100% wallet, sin pasarela.
 */
@Service
@RequiredArgsConstructor
public class CreatePaymentUseCaseImpl {

    private static final Logger log = LoggerFactory.getLogger(CreatePaymentUseCaseImpl.class);

    private final OrderRepository orderRepository;
    private final InitiateOrderPaymentUseCase initiateOrderPaymentUseCase;
    private final ChargeOrderViaGatewayUseCase chargeOrderViaGatewayUseCase;
    private final CreateWalletOnlyPaymentUseCase createWalletOnlyPaymentUseCase;
    private final GetWalletUseCaseImpl getWalletUseCase;
    private final ReserveWalletPaymentUseCaseImpl reserveWalletPaymentUseCase;
    private final ReleaseWalletPaymentUseCaseImpl releaseWalletPaymentUseCase;
    private final PaymentRepository paymentRepository;
    private final FailOrderPaymentUseCase failOrderPaymentUseCase;

    @Value("${payment.gateway}")
    private String gatewayName;

    /**
     * Orquesta la creación del pago. Sin {@code @Transactional} propio porque
     * incluye una llamada HTTP al gateway; cada operación de BD se hace en su
     * propia transacción delimitada.
     */
    public PaymentResponse execute(CreatePaymentRequest request, Long requesterId) {

        // Paso 1: validar y pasar orden a PAYMENT_PENDING en su propia transacción
        Order order = prepareOrder(request.getOrderId(), requesterId);

        String paymentMethod = request.getPaymentMethod() != null
            ? request.getPaymentMethod() : "QR";
        BigDecimal orderTotal = order.getTotalAmount().getAmount();

        // Flujo A: sin wallet
        if (!request.isApplyWalletCredit()) {
            log.info("CreatePayment (gateway-only): orderId={}, total={}", order.getOrderId(), orderTotal);
            try {
                return chargeOrderViaGatewayUseCase.execute(
                    order, orderTotal, paymentMethod, BigDecimal.ZERO, null);
            } catch (Exception e) {
                log.error("Gateway failed (gateway-only): orderId={}", order.getOrderId(), e);
                handleGatewayFailure(order, orderTotal, paymentMethod, null, e);
                throw new PaymentGatewayException(
                    "No se pudo procesar el pago. Por favor, intenta de nuevo.", e);
            }
        }

        // Flujo B/C: con wallet — consultar saldo disponible
        BigDecimal available = BigDecimal.ZERO;
        try {
            WalletResponse wallet = getWalletUseCase.execute(order.getUserId());
            available = wallet.getAvailableBalance();
        } catch (WalletNotFoundException e) {
            log.info("CreatePayment: no wallet for userId={}, falling back to gateway", order.getUserId());
        }

        if (available.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("CreatePayment: zero wallet balance for userId={}, gateway-only", order.getUserId());
            try {
                return chargeOrderViaGatewayUseCase.execute(
                    order, orderTotal, paymentMethod, BigDecimal.ZERO, null);
            } catch (Exception e) {
                log.error("Gateway failed (zero-wallet): orderId={}", order.getOrderId(), e);
                handleGatewayFailure(order, orderTotal, paymentMethod, null, e);
                throw new PaymentGatewayException(
                    "No se pudo procesar el pago. Por favor, intenta de nuevo.", e);
            }
        }

        BigDecimal amountToApply = orderTotal.min(available);
        BigDecimal remainder = orderTotal.subtract(amountToApply);

        // Reservar saldo en wallet en su propia transacción
        Long walletTxId = reserveWalletPaymentUseCase.execute(
            order.getUserId(), amountToApply, order.getOrderId());

        log.info("CreatePayment: wallet reserve created: userId={}, orderId={}, walletAmount={}, remainder={}",
            order.getUserId(), order.getOrderId(), amountToApply, remainder);

        // Flujo C: wallet cubre todo — sin pasarela
        if (remainder.compareTo(BigDecimal.ZERO) == 0) {
            log.info("CreatePayment (wallet-only): orderId={}", order.getOrderId());
            return createWalletOnlyPaymentUseCase.execute(order, walletTxId, amountToApply);
        }

        // Flujo B: pago mixto — cobrar el remainder por pasarela
        log.info("CreatePayment (mixed): orderId={}, gatewayAmount={}", order.getOrderId(), remainder);
        try {
            return chargeOrderViaGatewayUseCase.execute(
                order, remainder, paymentMethod, amountToApply, walletTxId);
        } catch (Exception e) {
            log.error("Gateway failed (mixed): orderId={}, walletTxId={}", order.getOrderId(), walletTxId, e);
            handleGatewayFailure(order, remainder, paymentMethod, walletTxId, e);
            throw new PaymentGatewayException(
                "No se pudo procesar el pago. Tu saldo de cartera ha sido liberado, intenta de nuevo.", e);
        }
    }

    /** Carga la orden, verifica que sea del solicitante y la pasa a PAYMENT_PENDING. */
    private Order prepareOrder(Long orderId, Long requesterId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!order.isOwnedBy(requesterId)) {
            throw new AccessDeniedException("You cannot pay this order");
        }
        initiateOrderPaymentUseCase.initiateOrderPayment(orderId);
        return order;
    }

    /**
     * Limpieza cuando el gateway falla.
     *
     * <p>Sin transacción propia: cada paso confirma en la de su bean, así el fallo de
     * uno no deshace los demás. Pasos:
     * <ol>
     *   <li>Liberar reserva wallet (si existe).
     *   <li>Crear Payment en ERROR para trazabilidad de auditoría.
     *   <li>Fallar la orden (cancela tickets y libera sillas a AVAILABLE).
     * </ol>
     */
    private void handleGatewayFailure(
            Order order,
            BigDecimal gatewayAmount,
            String paymentMethod,
            Long walletTxId,
            Exception cause) {

        String errorReason = "Gateway failure: " + cause.getMessage();

        // 1. Liberar reserva wallet si existe
        if (walletTxId != null) {
            try {
                releaseWalletPaymentUseCase.execute(walletTxId);
                log.info("Released wallet reservation: txId={}", walletTxId);
            } catch (Exception releaseEx) {
                log.error("Failed to release wallet reservation txId={}", walletTxId, releaseEx);
            }
        }

        // 2. Crear Payment en ERROR para auditoría
        try {
            Payment errorPayment = Payment.builder()
                .orderId(order.getOrderId())
                .gateway(PaymentGateway.valueOf(gatewayName.toUpperCase()))
                .gatewayTransactionId(null) // No se llegó a crear en el gateway
                .status(PaymentStatus.ERROR)
                .amount(new Money(gatewayAmount, order.getTotalAmount().getCurrency()))
                .paymentMethod(paymentMethod != null
                    ? PaymentMethod.valueOf(paymentMethod.toUpperCase())
                    : PaymentMethod.QR)
                .walletAmountApplied(BigDecimal.ZERO) // Ya se liberó la reserva
                .walletTransactionId(walletTxId)
                .build();

            paymentRepository.save(errorPayment);
            log.info("Created ERROR payment record: orderId={}", order.getOrderId());
        } catch (Exception saveEx) {
            log.error("Failed to save ERROR payment: orderId={}", order.getOrderId(), saveEx);
        }

        // 3. Fallar la orden (cancela tickets, libera sillas)
        try {
            failOrderPaymentUseCase.failOrderPayment(order.getOrderId(), errorReason);
            log.info("Failed order and released seats: orderId={}", order.getOrderId());
        } catch (Exception failEx) {
            log.error("Failed to fail order: orderId={}", order.getOrderId(), failEx);
        }
    }
}
