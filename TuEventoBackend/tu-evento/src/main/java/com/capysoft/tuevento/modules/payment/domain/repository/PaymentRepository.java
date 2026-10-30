package com.capysoft.tuevento.modules.payment.domain.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.capysoft.tuevento.modules.payment.application.dto.response.OrganizerEarningsByEventResponse;
import com.capysoft.tuevento.modules.payment.domain.model.Payment;
import com.capysoft.tuevento.modules.payment.domain.model.PaymentStatus;

/**
 * Repositorio del dominio para Payment.
 */
public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(Long paymentId);
    Optional<Payment> findByOrderId(Long orderId);
    Optional<Payment> findByGatewayTransactionId(String gatewayTransactionId);
    boolean existsById(Long paymentId);
    List<Payment> findByOrderIdInAndStatus(List<Long> orderIds, PaymentStatus status);

    /**
     * Pagos en PENDING cuyo {@code createdAt} es anterior a {@code olderThan}.
     * Usado por el job de conciliación para detectar pagos sin webhook.
     */
    List<Payment> findPendingOlderThan(LocalDateTime olderThan);

    /** Retorna las ganancias desglosadas de los eventos indicados. */
    List<OrganizerEarningsByEventResponse> findEarningsByEvents(Collection<Long> eventIds);
}
