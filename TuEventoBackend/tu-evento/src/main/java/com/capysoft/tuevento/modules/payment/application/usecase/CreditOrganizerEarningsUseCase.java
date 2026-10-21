package com.capysoft.tuevento.modules.payment.application.usecase;

import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.payment.domain.model.Payment;
import com.capysoft.tuevento.modules.ticket.domain.model.Order;
import com.capysoft.tuevento.modules.wallet.application.usecase.CreditWalletUseCaseImpl;
import com.capysoft.tuevento.modules.wallet.domain.model.WalletReferenceEntityType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Acredita en la wallet del organizador el monto total del pago aprobado.
 *
 * Corre en una transacción INDEPENDIENTE (REQUIRES_NEW) para que un fallo
 * aquí no revierta la transacción del webhook (pago del comprador).
 *
 * Idempotente: usa idempotencyKey = "organizer-earnings-payment-{paymentId}".
 */
@Service
@RequiredArgsConstructor
public class CreditOrganizerEarningsUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreditOrganizerEarningsUseCase.class);

    private final EventRepository eventRepository;
    private final CreditWalletUseCaseImpl creditWalletUseCase;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(Payment payment, Order order) {
        if (order == null) {
            log.warn("creditOrganizerEarnings: order is null for paymentId={}, skipping",
                    payment.getPaymentId());
            return;
        }

        Event event = eventRepository.findById(order.getEventId()).orElse(null);
        if (event == null) {
            log.warn("creditOrganizerEarnings: event not found for eventId={}, paymentId={}",
                    order.getEventId(), payment.getPaymentId());
            return;
        }

        Long organizerId = event.getUserId();

        BigDecimal gatewayAmount = payment.getAmount() != null
                ? payment.getAmount().getAmount() : BigDecimal.ZERO;
        BigDecimal walletApplied = payment.getWalletAmountApplied() != null
                ? payment.getWalletAmountApplied() : BigDecimal.ZERO;
        BigDecimal totalAmount = gatewayAmount.add(walletApplied);

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("creditOrganizerEarnings: totalAmount is zero for paymentId={}, skipping",
                    payment.getPaymentId());
            return;
        }

        String idempotencyKey = "organizer-earnings-payment-" + payment.getPaymentId();
        String reason = String.format("Ganancia por venta en evento '%s' (paymentId=%d)",
                event.getEventName(), payment.getPaymentId());

        creditWalletUseCase.execute(
                organizerId,
                totalAmount,
                WalletReferenceEntityType.PAYMENT_EARNINGS,
                payment.getPaymentId(),
                reason,
                idempotencyKey);

        log.info("Organizer wallet credited: organizerId={}, eventId={}, paymentId={}, amount={}",
                organizerId, order.getEventId(), payment.getPaymentId(), totalAmount);
    }
}
