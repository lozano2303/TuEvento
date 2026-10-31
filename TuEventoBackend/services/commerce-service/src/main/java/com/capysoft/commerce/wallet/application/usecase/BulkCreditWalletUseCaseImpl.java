package com.capysoft.commerce.wallet.application.usecase;

import com.capysoft.commerce.wallet.application.dto.BulkCreditWalletResponse;
import com.capysoft.commerce.wallet.application.port.out.PlatformUsersPort;
import com.capysoft.commerce.wallet.domain.model.WalletReferenceEntityType;
import com.capysoft.commerce.wallet.domain.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
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
 *
 * Recorrido:
 *   Por cursor sobre userId ascendente (no por número de página): no salta ni repite
 *   usuarios aunque se registren otros mientras corre.
 */
@Service
@RequiredArgsConstructor
public class BulkCreditWalletUseCaseImpl {

    private static final Logger log = LoggerFactory.getLogger(BulkCreditWalletUseCaseImpl.class);
    private static final int BATCH_SIZE = 50;

    private final PlatformUsersPort           platformUsersPort;
    private final CreditWalletUseCaseImpl     creditWalletUseCase;
    private final WalletTransactionRepository walletTransactionRepository;

    public BulkCreditWalletResponse execute(BigDecimal amount, String reason, String jobId) {

        String resolvedJobId = (jobId != null && !jobId.isBlank())
                ? jobId
                : "bulk-" + UUID.randomUUID();

        long totalUsers = platformUsersPort.countUsers();
        int  totalPages = (int) Math.ceil((double) totalUsers / BATCH_SIZE);

        int processed = 0;
        int skipped   = 0;
        int failed    = 0;

        log.info("BulkCreditWallet started: jobId={}, amount={}, totalUsers={}, batches={}",
                resolvedJobId, amount, totalUsers, totalPages);

        int batch = 0;
        int lastUserId = 0;
        List<Integer> userIds;
        while (!(userIds = platformUsersPort.findUserIdsAfter(lastUserId, BATCH_SIZE)).isEmpty()) {
            batch++;
            lastUserId = userIds.get(userIds.size() - 1);

            for (Integer id : userIds) {
                Long   userId          = id.longValue();
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

            log.debug("BulkCreditWallet batch {}/~{} done — processed={}, skipped={}, failed={}",
                    batch, totalPages, processed, skipped, failed);
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
