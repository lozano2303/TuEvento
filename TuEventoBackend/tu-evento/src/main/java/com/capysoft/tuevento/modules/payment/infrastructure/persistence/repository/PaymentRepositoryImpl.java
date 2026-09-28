package com.capysoft.tuevento.modules.payment.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.payment.domain.model.Payment;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentRepository;
import com.capysoft.tuevento.modules.payment.infrastructure.persistence.entity.PaymentEntity;
import com.capysoft.tuevento.modules.ticket.domain.model.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;

    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity = toEntity(payment);
        PaymentEntity savedEntity = jpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<Payment> findById(Long paymentId) {
        return jpaRepository.findById(paymentId).map(this::toDomain);
    }

    @Override
    public Optional<Payment> findByOrderId(Long orderId) {
        return jpaRepository.findByOrderId(orderId).map(this::toDomain);
    }

    @Override
    public Optional<Payment> findByGatewayTransactionId(String gatewayTransactionId) {
        return jpaRepository.findByGatewayTransactionId(gatewayTransactionId).map(this::toDomain);
    }

    @Override
    public boolean existsById(Long paymentId) {
        return jpaRepository.existsById(paymentId);
    }

    private PaymentEntity toEntity(Payment payment) {
        return PaymentEntity.builder()
            .paymentId(payment.getPaymentId())
            .orderId(payment.getOrderId())
            .gateway(payment.getGateway())
            .gatewayTransactionId(payment.getGatewayTransactionId())
            .status(payment.getStatus())
            .amount(payment.getAmount().getAmount())
            .currency(payment.getAmount().getCurrency())
            .paymentMethod(payment.getPaymentMethod())
            .processedAt(payment.getProcessedAt())
            .walletAmountApplied(payment.getWalletAmountApplied() != null
                ? payment.getWalletAmountApplied() : BigDecimal.ZERO)
            .walletTransactionId(payment.getWalletTransactionId())
            .build();
    }

    private Payment toDomain(PaymentEntity entity) {
        return Payment.builder()
            .paymentId(entity.getPaymentId())
            .orderId(entity.getOrderId())
            .gateway(entity.getGateway())
            .gatewayTransactionId(entity.getGatewayTransactionId())
            .status(entity.getStatus())
            .amount(new Money(entity.getAmount(), entity.getCurrency()))
            .paymentMethod(entity.getPaymentMethod())
            .processedAt(entity.getProcessedAt())
            .walletAmountApplied(entity.getWalletAmountApplied() != null
                ? entity.getWalletAmountApplied() : BigDecimal.ZERO)
            .walletTransactionId(entity.getWalletTransactionId())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }
}
