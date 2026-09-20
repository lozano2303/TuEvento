package com.capysoft.fakepaymentgateway.infrastructure.persistence;

import com.capysoft.fakepaymentgateway.domain.model.Money;
import com.capysoft.fakepaymentgateway.domain.model.Payment;
import com.capysoft.fakepaymentgateway.domain.repository.PaymentRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adaptador que implementa el repositorio del dominio usando JPA.
 */
@Component
public class PaymentRepositoryAdapter implements PaymentRepository {
    private final PaymentJpaRepository jpaRepository;
    
    public PaymentRepositoryAdapter(PaymentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity = toEntity(payment);
        PaymentEntity savedEntity = jpaRepository.save(entity);
        return toDomain(savedEntity);
    }
    
    @Override
    public Optional<Payment> findByPaymentId(String paymentId) {
        return jpaRepository.findByPaymentId(paymentId)
            .map(this::toDomain);
    }
    
    @Override
    public boolean existsByPaymentId(String paymentId) {
        return jpaRepository.existsByPaymentId(paymentId);
    }
    
    private PaymentEntity toEntity(Payment payment) {
        PaymentEntity entity = new PaymentEntity();
        entity.setPaymentId(payment.getPaymentId());
        entity.setExternalReference(payment.getExternalReference());
        entity.setStatus(payment.getStatus());
        entity.setAmount(payment.getMoney().getAmount());
        entity.setCurrency(payment.getMoney().getCurrency());
        entity.setPaymentMethod(payment.getPaymentMethod());
        entity.setCreatedAt(payment.getCreatedAt());
        entity.setUpdatedAt(payment.getUpdatedAt());
        entity.setWebhookEventId(payment.getWebhookEventId());
        return entity;
    }
    
    private Payment toDomain(PaymentEntity entity) {
        Payment payment = new Payment();
        payment.setPaymentId(entity.getPaymentId());
        payment.setExternalReference(entity.getExternalReference());
        payment.setStatus(entity.getStatus());
        payment.setMoney(new Money(entity.getAmount(), entity.getCurrency()));
        payment.setPaymentMethod(entity.getPaymentMethod());
        payment.setCreatedAt(entity.getCreatedAt());
        payment.setUpdatedAt(entity.getUpdatedAt());
        payment.setWebhookEventId(entity.getWebhookEventId());
        return payment;
    }
}
