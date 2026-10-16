package com.capysoft.tuevento.modules.wallet.application.usecase;

import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.UserJpaRepository;
import com.capysoft.tuevento.modules.wallet.application.dto.BulkCreditWalletResponse;
import com.capysoft.tuevento.modules.wallet.domain.model.WalletReferenceEntityType;
import com.capysoft.tuevento.modules.wallet.domain.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Acredita saldo a TODOS los usuarios de la plataforma en lotes de 50.
 *
 * Idempotencia:
 *   Cada usuario tiene una clave única: "bulk-{jobId}-{userId}".
 *   Antes de acreditar se verifica si esa clave ya existe — si sí, se cuenta
 *   como 'skipped' y no se genera crédito duplicado.
 *   Si el admin reenvía la misma request con el mismo jobId, nadie recibe doble.
 *
 * Resiliencia:
 *   Si un usuario falla (wallet corrupta, lock, etc.), se registra en 'failed'
 *   y el proceso continúa con el siguiente — no hay rollback global.
 */
@Service
@RequiredArgsConstructor
public class BulkCreditWalletUseCaseImpl {

    private static final Logger log = LoggerFactory.getLogger(BulkCreditWalletUseCaseImpl.class);
    private static final int BATCH_SIZE = 50;

    private final UserJpaRepository           userJpaRepository;
    private final CreditWalletUseCaseImpl     creditWalletUseCase;
    private final WalletTransactionRepository walletTransactionRepository;

    public BulkCreditWalletResponse execute(BigDecimal amount, String reason, String jobId) {

        String resolvedJobId = (jobId != null && !jobId.isBlank())
                ? jobId
                : "bulk-" + UUID.randomUUID();

        long totalUsers = userJpaRepository.count();
        int  totalPages = (int) Math.ceil((double) totalUsers / BATCH_SIZE);

        int processed = 0;
        int skipped   = 0;
        int failed    = 0;

        log.info("BulkCreditWallet started: jobId={}, amount={}, totalUsers={}, batches={}",
                resolvedJobId, amount, totalUsers, totalPages);

        for (int page = 0; page < totalPages; page++) {
            var users = userJpaRepository.findAll(PageRequest.of(page, BATCH_SIZE));

            for (var user : users) {
                Long   userId          = user.getUserId().longValue();
                String idempotencyKey  = "bulk-" + resolvedJobId + "-" + userId;

                // ── Verificar idempotencia antes de acreditar ─────────────────
                if (walletTransactionRepository.existsByIdempotencyKey(idempotencyKey)) {
                    skipped++;
                    log.debug("BulkCreditWallet skip (idempotent): userId={}, jobId={}", userId, resolvedJobId);
                    continue;
                }

                // ── Acreditar ────────────────────────────────────────────────
                try {
                    creditWalletUseCase.execute(
                            userId,
                            amount,
                            WalletReferenceEntityType.ADMIN_ADJUSTMENT,
                            null,
                            reason,
                            idempotencyKey
                    );
                    processed++;
                } catch (Exception e) {
                    failed++;
                    log.warn("BulkCreditWallet failed for userId={}, jobId={}: {}",
                            userId, resolvedJobId, e.getMessage());
                }
            }

            log.debug("BulkCreditWallet batch {}/{} done — processed={}, skipped={}, failed={}",
                    page + 1, totalPages, processed, skipped, failed);
        }

        log.info("BulkCreditWallet finished: jobId={}, processed={}, skipped={}, failed={}",
                resolvedJobId, processed, skipped, failed);

        return BulkCreditWalletResponse.builder()
                .jobId(resolvedJobId)
                .usersProcessed(processed)
                .skipped(skipped)
                .failed(failed)
                .amountPerUser(amount)
                .reason(reason)
                .build();
    }
}
