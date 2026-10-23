package com.capysoft.tuevento.modules.payment.application.usecase;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * Flujo A — applyWalletCredit=false (o usuario sin saldo):
 *   Delega a ChargeOrderViaGatewayUseCase con el total de la orden.
 *   Idéntico al comportamiento anterior.
 *
 * Flujo B — applyWalletCredit=true, saldo parcial (0 < saldo < total):
 *   1. Consulta saldo disponible
 *   2. Reserva min(total, disponible) en wallet (PAYMENT PENDING)
 *   3. Delega a ChargeOrderViaGatewayUseCase con el remainder
 *   4. Si (3) lanza excepción, toda la transacción hace rollback
 *      (la reserva queda sin persistir — no hay reserva huérfana)
 *
 * Flujo C — applyWalletCredit=true, saldo >= total:
 *   1. Reserva el total en wallet
 *   2. Delega a CreateWalletOnlyPaymentUseCase (APPROVED al instante, sin pasarela)
 *
 * Límite transaccional:
 *   Todo ocurre en una sola @Transactional. Si ChargeOrderViaGatewayUseCase
 *   falla (ej. gateway no responde), el rollback deshace la reserva wallet y
 *   la transición de la orden — no queda ningún estado huérfano.
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

    @Transactional
    public PaymentResponse execute(CreatePaymentRequest request) {
        // 1. Cargar orden
        Order order = orderRepository.findById(request.getOrderId())
            .orElseThrow(() -> new OrderNotFoundException(request.getOrderId()));

        // 2. Transicionar orden a PAYMENT_PENDING
        initiateOrderPaymentUseCase.initiateOrderPayment(request.getOrderId());

        String paymentMethod = request.getPaymentMethod() != null
            ? request.getPaymentMethod() : "QR";
        BigDecimal orderTotal = order.getTotalAmount().getAmount();

        // 3. Sin wallet — flujo idéntico al anterior
        if (!request.isApplyWalletCredit()) {
            log.info("CreatePayment (gateway-only): orderId={}, total={}", order.getOrderId(), orderTotal);
            try {
                return chargeOrderViaGatewayUseCase.execute(
                    order, orderTotal, paymentMethod, BigDecimal.ZERO, null);
            } catch (Exception e) {
                log.error("Gateway failed for gateway-only payment: orderId={}", order.getOrderId(), e);
                handleGatewayFailure(order, orderTotal, paymentMethod, null, e);
                throw new PaymentGatewayException(
                    "No se pudo procesar el pago. Por favor, intenta de nuevo.", e);
            }
        }

        // 4. Con wallet — consultar saldo disponible
        BigDecimal available = BigDecimal.ZERO;
        try {
            WalletResponse wallet = getWalletUseCase.execute(order.getUserId());
            available = wallet.getAvailableBalance();
        } catch (WalletNotFoundException e) {
            // Usuario sin wallet → tratar como saldo 0 → flujo gateway normal
            log.info("CreatePayment: no wallet for userId={}, falling back to gateway", order.getUserId());
        }

        if (available.compareTo(BigDecimal.ZERO) <= 0) {
            // Saldo 0 → flujo gateway normal
            log.info("CreatePayment: zero wallet balance for userId={}, gateway-only", order.getUserId());
            try {
                return chargeOrderViaGatewayUseCase.execute(
                    order, orderTotal, paymentMethod, BigDecimal.ZERO, null);
            } catch (Exception e) {
                log.error("Gateway failed for zero-wallet payment: orderId={}", order.getOrderId(), e);
                handleGatewayFailure(order, orderTotal, paymentMethod, null, e);
                throw new PaymentGatewayException(
                    "No se pudo procesar el pago. Por favor, intenta de nuevo.", e);
            }
        }

        // amountToApply = min(total, disponible)
        BigDecimal amountToApply = orderTotal.min(available);
        BigDecimal remainder = orderTotal.subtract(amountToApply);

        // 5. Reservar saldo en wallet (PAYMENT PENDING)
        Long walletTxId = reserveWalletPaymentUseCase.execute(
            order.getUserId(), amountToApply, order.getOrderId());

        log.info("CreatePayment: wallet reserve created: userId={}, orderId={}, walletAmount={}, remainder={}",
            order.getUserId(), order.getOrderId(), amountToApply, remainder);

        // 6a. Wallet cubre todo — sin pasarela
        if (remainder.compareTo(BigDecimal.ZERO) == 0) {
            log.info("CreatePayment (wallet-only): orderId={}", order.getOrderId());
            return createWalletOnlyPaymentUseCase.execute(order, walletTxId, amountToApply);
        }

        // 6b. Pago mixto — cobrar el remainder por pasarela
        // Si el gateway falla, limpiar la reserva wallet y marcar payment/order en ERROR.
        log.info("CreatePayment (mixed): orderId={}, gatewayAmount={}", order.getOrderId(), remainder);
        try {
            return chargeOrderViaGatewayUseCase.execute(
                order, remainder, paymentMethod, amountToApply, walletTxId);
        } catch (Exception e) {
            log.error("Gateway failed for mixed payment: orderId={}, walletTxId={}", 
                order.getOrderId(), walletTxId, e);
            handleGatewayFailure(order, remainder, paymentMethod, walletTxId, e);
            throw new PaymentGatewayException(
                "No se pudo procesar el pago. Tu saldo de cartera ha sido liberado, intenta de nuevo.", e);
        }
    }

    /**
     * Maneja la limpieza cuando el gateway falla:
     * 1. Si hay wallet reservada (walletTxId != null), libérala (PENDING → FAILED)
     * 2. Crea un Payment en ERROR para dejar trazabilidad
     * 3. Falla la orden (cancela tickets, libera sillas a AVAILABLE)
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
