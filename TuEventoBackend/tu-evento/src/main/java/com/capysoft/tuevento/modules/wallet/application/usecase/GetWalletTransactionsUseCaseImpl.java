package com.capysoft.tuevento.modules.wallet.application.usecase;

import com.capysoft.tuevento.modules.wallet.application.dto.WalletTransactionResponse;
import com.capysoft.tuevento.modules.wallet.domain.model.Wallet;
import com.capysoft.tuevento.modules.wallet.domain.model.WalletNotFoundException;
import com.capysoft.tuevento.modules.wallet.domain.repository.WalletRepository;
import com.capysoft.tuevento.modules.wallet.domain.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Retorna el historial de movimientos de la wallet del usuario.
 */
@Service
@RequiredArgsConstructor
public class GetWalletTransactionsUseCaseImpl {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> execute(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
            .orElseThrow(() -> new WalletNotFoundException(userId));

        return walletTransactionRepository.findByWalletId(wallet.getWalletId())
            .stream()
            .map(WalletTransactionResponse::fromDomain)
            .collect(Collectors.toList());
    }
}
