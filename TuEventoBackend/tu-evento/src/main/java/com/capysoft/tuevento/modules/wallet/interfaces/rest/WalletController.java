package com.capysoft.tuevento.modules.wallet.interfaces.rest;

import com.capysoft.tuevento.modules.wallet.application.dto.*;
import com.capysoft.tuevento.modules.wallet.application.usecase.*;
import com.capysoft.tuevento.modules.wallet.domain.model.WalletReferenceEntityType;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityUser;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para el módulo wallet.
 *
 * Rutas:
 *   GET  /api/v1/wallet/me                — balance + disponible del usuario autenticado
 *   GET  /api/v1/wallet/me/transactions   — historial del usuario autenticado
 *   POST /api/v1/wallet/credit            — ADMIN: acreditar saldo
 *   POST /api/v1/wallet/adjust            — ADMIN: ajuste manual (±)
 */
@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final GetWalletUseCaseImpl getWalletUseCase;
    private final GetWalletTransactionsUseCaseImpl getWalletTransactionsUseCase;
    private final CreditWalletUseCaseImpl creditWalletUseCase;
    private final AdjustWalletUseCaseImpl adjustWalletUseCase;

    /**
     * Retorna el balance total y disponible de la wallet del usuario autenticado.
     * Si el usuario no tiene wallet aún, retorna 404.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet(
            @AuthenticationPrincipal SecurityUser securityUser) {
        Long userId = securityUser.getUserId().longValue();
        WalletResponse response = getWalletUseCase.execute(userId);
        return ResponseEntity.ok(ApiResponse.ok("Wallet retrieved successfully", response));
    }

    /**
     * Retorna el historial de movimientos de la wallet del usuario autenticado.
     */
    @GetMapping("/me/transactions")
    public ResponseEntity<ApiResponse<List<WalletTransactionResponse>>> getMyTransactions(
            @AuthenticationPrincipal SecurityUser securityUser) {
        Long userId = securityUser.getUserId().longValue();
        List<WalletTransactionResponse> transactions = getWalletTransactionsUseCase.execute(userId);
        return ResponseEntity.ok(ApiResponse.ok("Transactions retrieved successfully", transactions));
    }

    /**
     * Acredita saldo en la wallet de un usuario (uso manual/ops — solo ADMIN).
     * Genera un idempotencyKey único por request.
     */
    @PostMapping("/credit")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<WalletResponse>> credit(
            @Valid @RequestBody CreditWalletRequest request) {
        String idempotencyKey = "manual-credit-" + UUID.randomUUID();
        WalletResponse response = creditWalletUseCase.execute(
            request.getUserId(),
            request.getAmount(),
            WalletReferenceEntityType.ORDER, // referencia genérica para crédito manual
            0L,
            request.getReason(),
            idempotencyKey
        );
        return ResponseEntity.ok(ApiResponse.ok("Wallet credited successfully", response));
    }

    /**
     * Ajuste administrativo de balance (solo ADMIN).
     * amount positivo suma, negativo resta. Nunca deja el balance negativo.
     */
    @PostMapping("/adjust")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<WalletResponse>> adjust(
            @Valid @RequestBody AdjustWalletRequest request,
            @AuthenticationPrincipal SecurityUser securityUser) {
        String createdBy = securityUser.getUsername();
        WalletResponse response = adjustWalletUseCase.execute(
            request.getUserId(),
            request.getAmount(),
            request.getReason(),
            createdBy
        );
        return ResponseEntity.ok(ApiResponse.ok("Wallet adjusted successfully", response));
    }
}
