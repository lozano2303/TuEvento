package com.capysoft.tuevento.modules.wallet.application.usecase;

import com.capysoft.tuevento.modules.wallet.application.dto.WalletResponse;
import com.capysoft.tuevento.modules.wallet.domain.model.Wallet;
import com.capysoft.tuevento.modules.wallet.domain.model.WalletNotFoundException;
import com.capysoft.tuevento.modules.wallet.domain.repository.WalletRepository;
import com.capysoft.tuevento.modules.wallet.domain.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Retorna balance total y disponible (balance - PAYMENTs PENDING) de la wallet del usuario.
 */
@Service
@RequiredArgsConstructor
public class GetWalletUseCaseImpl {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Transactional(readOnly = true)
    public WalletResponse execute(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
            .orElseThrow(() -> new WalletNotFoundException(userId));

        BigDecimal pendingPayments = walletTransactionRepository.sumPendingPayments(wallet.getWalletId());
        return WalletResponse.fromDomain(wallet, pendingPayments);
    }
}
