package com.capysoft.tuevento.modules.payment.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.payment.domain.model.Refund;
import com.capysoft.tuevento.modules.payment.domain.repository.RefundRepository;
import com.capysoft.tuevento.modules.payment.infrastructure.persistence.entity.RefundEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RefundRepositoryImpl implements RefundRepository {
    
    private final RefundJpaRepository jpaRepository;
    
    @Override
    public Refund save(Refund refund) {
        RefundEntity entity = toEntity(refund);
        RefundEntity savedEntity = jpaRepository.save(entity);
        return toDomain(savedEntity);
    }
    
    @Override
    public Optional<Refund> findById(Long refundId) {
        return jpaRepository.findById(refundId).map(this::toDomain);
    }
    
    @Override
    public Optional<Refund> findByPaymentId(Long paymentId) {
        return jpaRepository.findByPaymentId(paymentId).map(this::toDomain);
    }
    
    private RefundEntity toEntity(Refund refund) {
        return RefundEntity.builder()
            .refundId(refund.getRefundId())
            .paymentId(refund.getPaymentId())
            .status(refund.getStatus())
            .reason(refund.getReason())
            .requestedAt(refund.getRequestedAt())
            .processedAt(refund.getProcessedAt())
            .build();
    }
    
    private Refund toDomain(RefundEntity entity) {
        return Refund.builder()
            .refundId(entity.getRefundId())
            .paymentId(entity.getPaymentId())
            .status(entity.getStatus())
            .reason(entity.getReason())
            .requestedAt(entity.getRequestedAt())
            .processedAt(entity.getProcessedAt())
            .build();
    }
}
