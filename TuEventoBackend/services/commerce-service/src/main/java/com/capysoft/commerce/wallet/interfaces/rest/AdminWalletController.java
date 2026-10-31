package com.capysoft.commerce.wallet.interfaces.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.capysoft.commerce.wallet.application.dto.BulkCreditWalletRequest;
import com.capysoft.commerce.wallet.application.dto.BulkCreditWalletResponse;
import com.capysoft.commerce.wallet.application.usecase.BulkCreditWalletUseCaseImpl;
import com.capysoft.common.interfaces.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Crédito masivo a carteras. Vive en Comercio; antes estaba en el AdminController de Identidad. */
@RestController
@RequestMapping("/api/v1/admin/wallet")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Admin", description = "Administration endpoints — requires ADMIN role")
public class AdminWalletController {

    private final BulkCreditWalletUseCaseImpl bulkCreditWalletUseCase;

    @Operation(summary = "Credit wallet balance to ALL platform users — idempotent via jobId")
    @PostMapping("/credit-all")
    public ResponseEntity<ApiResponse<BulkCreditWalletResponse>> bulkCreditAll(
            @Valid @RequestBody BulkCreditWalletRequest request) {
        BulkCreditWalletResponse result = bulkCreditWalletUseCase.execute(
                request.getAmount(),
                request.getReason(),
                request.getJobId()
        );
        return ResponseEntity.ok(ApiResponse.ok("Bulk wallet credit completed", result));
    }
}
