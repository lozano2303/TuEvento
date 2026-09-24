package com.capysoft.tuevento.modules.payment.application.usecase;

import com.capysoft.tuevento.modules.payment.application.dto.request.CreatePaymentRequest;
import com.capysoft.tuevento.modules.payment.application.dto.response.PaymentResponse;
import com.capysoft.tuevento.modules.ticket.application.port.in.InitiateOrderPaymentUseCase;
import com.capysoft.tuevento.modules.ticket.domain.model.Order;
import com.capysoft.tuevento.modules.ticket.domain.model.OrderNotFoundException;
import com.capysoft.tuevento.modules.ticket.domain.repository.OrderRepository;
import com.capysoft.tuevento.modules.wallet.application.dto.WalletResponse;
import com.capysoft.tuevento.modules.wallet.application.usecase.GetWalletUseCaseImpl;
import com.capysoft.tuevento.modules.wallet.application.usecase.ReserveWalletPaymentUseCaseImpl;
import com.capysoft.tuevento.modules.wallet.domain.model.WalletNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

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
            return chargeOrderViaGatewayUseCase.execute(
                order, orderTotal, paymentMethod, BigDecimal.ZERO, null);
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
            return chargeOrderViaGatewayUseCase.execute(
                order, orderTotal, paymentMethod, BigDecimal.ZERO, null);
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
        // Si el gateway falla, toda la transacción hace rollback (reserva incluida).
        log.info("CreatePayment (mixed): orderId={}, gatewayAmount={}", order.getOrderId(), remainder);
        return chargeOrderViaGatewayUseCase.execute(
            order, remainder, paymentMethod, amountToApply, walletTxId);
    }
}
