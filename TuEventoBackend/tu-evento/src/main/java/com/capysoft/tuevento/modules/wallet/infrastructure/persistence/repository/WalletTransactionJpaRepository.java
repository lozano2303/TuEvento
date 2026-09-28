package com.capysoft.tuevento.modules.wallet.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.wallet.domain.model.WalletTransactionStatus;
import com.capysoft.tuevento.modules.wallet.domain.model.WalletTransactionType;
import com.capysoft.tuevento.modules.wallet.infrastructure.persistence.entity.WalletTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface WalletTransactionJpaRepository extends JpaRepository<WalletTransactionEntity, Long> {
    Optional<WalletTransactionEntity> findByIdempotencyKey(String idempotencyKey);
    boolean existsByIdempotencyKey(String idempotencyKey);
    List<WalletTransactionEntity> findByWalletIdOrderByCreatedAtDesc(Long walletId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransactionEntity t " +
           "WHERE t.walletId = :walletId AND t.type = :type AND t.status = :status")
    BigDecimal sumByWalletIdAndTypeAndStatus(
        @Param("walletId") Long walletId,
        @Param("type") WalletTransactionType type,
        @Param("status") WalletTransactionStatus status
    );
}
