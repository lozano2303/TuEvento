package com.capysoft.tuevento.modules.payment.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.payment.domain.model.PaymentLog;
import com.capysoft.tuevento.modules.payment.domain.repository.PaymentLogRepository;
import com.capysoft.tuevento.modules.payment.infrastructure.persistence.entity.PaymentLogEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PaymentLogRepositoryImpl implements PaymentLogRepository {
    
    private final PaymentLogJpaRepository jpaRepository;
    
    @Override
    public PaymentLog save(PaymentLog log) {
        PaymentLogEntity entity = toEntity(log);
        PaymentLogEntity savedEntity = jpaRepository.save(entity);
        return toDomain(savedEntity);
    }
    
    @Override
    public List<PaymentLog> findByPaymentId(Long paymentId) {
        return jpaRepository.findByPaymentId(paymentId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    private PaymentLogEntity toEntity(PaymentLog log) {
        return PaymentLogEntity.builder()
            .paymentLogId(log.getPaymentLogId())
            .paymentId(log.getPaymentId())
            .oldStatus(log.getOldStatus())
            .newStatus(log.getNewStatus())
            .changedAt(log.getChangedAt())
            .reason(log.getReason())
            .build();
    }
    
    private PaymentLog toDomain(PaymentLogEntity entity) {
        return PaymentLog.builder()
            .paymentLogId(entity.getPaymentLogId())
            .paymentId(entity.getPaymentId())
            .oldStatus(entity.getOldStatus())
            .newStatus(entity.getNewStatus())
            .changedAt(entity.getChangedAt())
            .reason(entity.getReason())
            .build();
    }
}
