package com.capysoft.tuevento.modules.wallet.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.wallet.domain.model.WalletTransaction;
import com.capysoft.tuevento.modules.wallet.domain.model.WalletTransactionStatus;
import com.capysoft.tuevento.modules.wallet.domain.model.WalletTransactionType;
import com.capysoft.tuevento.modules.wallet.domain.repository.WalletTransactionRepository;
import com.capysoft.tuevento.modules.wallet.infrastructure.persistence.entity.WalletTransactionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WalletTransactionRepositoryImpl implements WalletTransactionRepository {

    private final WalletTransactionJpaRepository jpaRepository;

    @Override
    public WalletTransaction save(WalletTransaction transaction) {
        WalletTransactionEntity entity = toEntity(transaction);
        WalletTransactionEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<WalletTransaction> findById(Long transactionId) {
        return jpaRepository.findById(transactionId).map(this::toDomain);
    }

    @Override
    public Optional<WalletTransaction> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey).map(this::toDomain);
    }

    @Override
    public boolean existsByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.existsByIdempotencyKey(idempotencyKey);
    }

    @Override
    public List<WalletTransaction> findByWalletId(Long walletId) {
        return jpaRepository.findByWalletIdOrderByCreatedAtDesc(walletId)
            .stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public BigDecimal sumPendingPayments(Long walletId) {
        return jpaRepository.sumByWalletIdAndTypeAndStatus(
            walletId,
            WalletTransactionType.PAYMENT,
            WalletTransactionStatus.PENDING
        );
    }

    private WalletTransactionEntity toEntity(WalletTransaction t) {
        return WalletTransactionEntity.builder()
            .transactionId(t.getTransactionId())
            .walletId(t.getWalletId())
            .type(t.getType())
            .amount(t.getAmount())
            .status(t.getStatus())
            .balanceAfter(t.getBalanceAfter())
            .idempotencyKey(t.getIdempotencyKey())
            .createdAt(t.getCreatedAt())
            .createdBy(t.getCreatedBy())
            .build();
    }

    private WalletTransaction toDomain(WalletTransactionEntity e) {
        return WalletTransaction.builder()
            .transactionId(e.getTransactionId())
            .walletId(e.getWalletId())
            .type(e.getType())
            .amount(e.getAmount())
            .status(e.getStatus())
            .balanceAfter(e.getBalanceAfter())
            .idempotencyKey(e.getIdempotencyKey())
            .createdAt(e.getCreatedAt())
            .createdBy(e.getCreatedBy())
            .build();
    }
}
