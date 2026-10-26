package com.capysoft.tuevento.modules.payment.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.capysoft.tuevento.modules.event.application.dto.response.OrganizerEarningsByEventResponse;
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

    /** Pagos PENDING creados antes del umbral indicado (para conciliación). */
    List<Payment> findPendingOlderThan(LocalDateTime threshold);

    /** Retorna las ganancias desglosadas por evento para el organizador indicado. */
    List<OrganizerEarningsByEventResponse> findEarningsByOrganizer(Long organizerId);
}
