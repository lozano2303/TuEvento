package com.capysoft.tuevento.modules.wallet.application.usecase;

import com.capysoft.tuevento.modules.wallet.application.dto.WalletResponse;
import com.capysoft.tuevento.modules.wallet.domain.model.*;
import com.capysoft.tuevento.modules.wallet.domain.repository.WalletReferenceRepository;
import com.capysoft.tuevento.modules.wallet.domain.repository.WalletRepository;
import com.capysoft.tuevento.modules.wallet.domain.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Acredita saldo en la wallet de un usuario.
 *
 * Expuesto como puerto para ser invocado en-proceso por la orquestación de
 * cancelación de evento (módulo externo). También expuesto por REST (solo ADMIN).
 *
 * Es IDEMPOTENTE: si ya existe una transacción con el mismo idempotencyKey,
 * devuelve la wallet actual sin crear un duplicado.
 *
 * Flujo:
 *  1. Obtener (o crear) la wallet del usuario
 *  2. Verificar idempotencia
 *  3. Crear WalletTransaction CREDIT en COMPLETED
 *  4. Actualizar balance del Wallet (crédito directo a COMPLETED)
 *  5. Crear WalletReference para trazabilidad
 */
@Service
@RequiredArgsConstructor
public class CreditWalletUseCaseImpl {

    private static final Logger log = LoggerFactory.getLogger(CreditWalletUseCaseImpl.class);

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletReferenceRepository walletReferenceRepository;

    @Transactional
    public WalletResponse execute(
            Long userId,
            BigDecimal amount,
            WalletReferenceEntityType entityType,
            Long entityId,
            String reason,
            String idempotencyKey) {

        // 1. Idempotencia — si ya fue procesado devolver estado actual
        if (walletTransactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.info("CreditWallet idempotent skip: idempotencyKey={}, userId={}", idempotencyKey, userId);
            Wallet existing = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException(userId));
            BigDecimal pending = walletTransactionRepository.sumPendingPayments(existing.getWalletId());
            return WalletResponse.fromDomain(existing, pending);
        }

        // 2. Obtener o crear wallet (lazy creation)
        Wallet wallet = walletRepository.findByUserId(userId)
            .orElseGet(() -> walletRepository.save(Wallet.builder()
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .currency("COP")
                .version(0L)
                .build()));

        // 3. Crear transacción CREDIT directo a COMPLETED
        BigDecimal newBalance = wallet.getBalance().add(amount);
        WalletTransaction transaction = WalletTransaction.builder()
            .walletId(wallet.getWalletId())
            .type(WalletTransactionType.CREDIT)
            .amount(amount)
            .status(WalletTransactionStatus.COMPLETED)
            .balanceAfter(newBalance)
            .idempotencyKey(idempotencyKey)
            .build();
        WalletTransaction savedTx = walletTransactionRepository.save(transaction);

        // 4. Actualizar balance
        Wallet credited = wallet.credit(amount);
        Wallet savedWallet = walletRepository.save(credited);

        // 5. Crear referencia de trazabilidad
        WalletReference reference = WalletReference.builder()
            .transactionId(savedTx.getTransactionId())
            .entityType(entityType)
            .entityId(entityId)
            .build();
        walletReferenceRepository.save(reference);

        log.info("Wallet credited: userId={}, amount={}, walletId={}, txId={}, reason={}",
            userId, amount, savedWallet.getWalletId(), savedTx.getTransactionId(), reason);

        BigDecimal pending = walletTransactionRepository.sumPendingPayments(savedWallet.getWalletId());
        return WalletResponse.fromDomain(savedWallet, pending);
    }
}
