package com.capysoft.tuevento.modules.payment.application.usecase;

import com.capysoft.tuevento.modules.payment.application.dto.response.PaymentResponse;
import com.capysoft.tuevento.modules.payment.domain.model.*;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentLogRepository;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentRepository;
import com.capysoft.tuevento.modules.ticket.application.port.in.ConfirmOrderPaymentUseCase;
import com.capysoft.tuevento.modules.ticket.domain.model.Money;
import com.capysoft.tuevento.modules.ticket.domain.model.Order;
import com.capysoft.tuevento.modules.wallet.application.usecase.ConfirmWalletPaymentUseCaseImpl;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Caso de uso para pagos 100% con saldo de wallet (sin pasarela).
 *
 * Cuando el saldo cubre el total de la orden:
 *  1. Crea Payment directamente en APPROVED con amount=0 y walletAmountApplied=total
 *  2. En la misma transacción confirma la orden (ConfirmOrderPaymentUseCase)
 *     y la reserva wallet (ConfirmWalletPaymentUseCaseImpl)
 *  3. No hay webhook — todo se resuelve sincrónicamente.
 *
 * gatewayTransactionId usa el prefijo "wallet_only_" + UUID para identificar
 * estos pagos sin pasarela.
 */
@Service
@RequiredArgsConstructor
public class CreateWalletOnlyPaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateWalletOnlyPaymentUseCase.class);

    private final PaymentRepository paymentRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final ConfirmOrderPaymentUseCase confirmOrderPaymentUseCase;
    private final ConfirmWalletPaymentUseCaseImpl confirmWalletPaymentUseCase;

    @Value("${payment.gateway}")
    private String gatewayName;

    /**
     * @param order               La orden ya en PAYMENT_PENDING
     * @param walletTransactionId ID de la reserva PENDING en la wallet
     * @param walletAmount        Monto total de la orden (cubierto por wallet)
     */
    @Transactional
    public PaymentResponse execute(Order order, Long walletTransactionId, BigDecimal walletAmount) {
        String gatewayTxId = "wallet_only_" + UUID.randomUUID();

        // 1. Crear Payment en APPROVED directamente (amount=0, walletAmountApplied=total)
        Payment payment = Payment.builder()
            .orderId(order.getOrderId())
            .gateway(PaymentGateway.valueOf(gatewayName.toUpperCase()))
            .gatewayTransactionId(gatewayTxId)
            .status(PaymentStatus.APPROVED)
            .amount(new Money(BigDecimal.ZERO, order.getTotalAmount().getCurrency()))
            .paymentMethod(PaymentMethod.QR) // método irrelevante, no hay pasarela
            .walletAmountApplied(walletAmount)
            .walletTransactionId(walletTransactionId)
            .processedAt(LocalDateTime.now())
            .build();
        Payment savedPayment = paymentRepository.save(payment);

        // 2. Log de auditoría
        PaymentLog paymentLog = PaymentLog.builder()
            .paymentId(savedPayment.getPaymentId())
            .oldStatus(PaymentStatus.PENDING)
            .newStatus(PaymentStatus.APPROVED)
            .changedAt(LocalDateTime.now())
            .reason("Wallet-only payment — no gateway involved")
            .build();
        paymentLogRepository.save(paymentLog);

        // 3. Confirmar orden y tickets (PAYMENT_PENDING → PAID)
        confirmOrderPaymentUseCase.confirmOrderPayment(order.getOrderId(), gatewayTxId);

        // 4. Confirmar reserva wallet (PENDING → COMPLETED, descuenta balance)
        confirmWalletPaymentUseCase.execute(walletTransactionId);

        log.info("Wallet-only payment completed: orderId={}, walletTxId={}, amount={}",
            order.getOrderId(), walletTransactionId, walletAmount);

        return PaymentResponse.fromDomain(savedPayment);
    }
}
