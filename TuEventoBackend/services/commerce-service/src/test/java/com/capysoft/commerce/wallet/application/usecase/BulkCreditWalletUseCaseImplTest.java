package com.capysoft.commerce.wallet.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.capysoft.commerce.wallet.application.dto.BulkCreditWalletResponse;
import com.capysoft.commerce.wallet.application.port.out.PlatformUsersPort;
import com.capysoft.commerce.wallet.domain.model.WalletReferenceEntityType;
import com.capysoft.commerce.wallet.domain.repository.WalletTransactionRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("BulkCreditWalletUseCaseImpl")
class BulkCreditWalletUseCaseImplTest {

    private static final BigDecimal AMOUNT = new BigDecimal("5000");

    @Mock PlatformUsersPort platformUsersPort;
    @Mock CreditWalletUseCaseImpl creditWalletUseCase;
    @Mock WalletTransactionRepository walletTransactionRepository;
    @InjectMocks BulkCreditWalletUseCaseImpl useCase;

    @Test
    @DisplayName("recorre todos los usuarios por cursor, lote tras lote, y acredita a cada uno una vez")
    void creditsEveryUserAcrossBatches() {
        List<Integer> firstBatch = IntStream.rangeClosed(1, 50).boxed().toList();
        when(platformUsersPort.countUsers()).thenReturn(53L);
        when(platformUsersPort.findUserIdsAfter(0, 50)).thenReturn(firstBatch);
        when(platformUsersPort.findUserIdsAfter(50, 50)).thenReturn(List.of(60, 61, 70));
        when(platformUsersPort.findUserIdsAfter(70, 50)).thenReturn(List.of());

        BulkCreditWalletResponse response = useCase.execute(AMOUNT, "Bono", "job-1");

        assertThat(response.getUsersProcessed()).isEqualTo(53);
        assertThat(response.getSkipped()).isZero();
        assertThat(response.getFailed()).isZero();
        verify(creditWalletUseCase).execute(eq(70L), eq(AMOUNT), eq(WalletReferenceEntityType.ADMIN_ADJUSTMENT),
                isNull(), eq("Bono"), eq("bulk-job-1-70"));
        verify(creditWalletUseCase, times(53)).execute(any(), any(), any(), any(), any(), anyString());
    }

    @Test
    @DisplayName("repetir el mismo jobId no acredita dos veces y un fallo no detiene el resto")
    void skipsAlreadyCreditedAndCountsFailures() {
        when(platformUsersPort.countUsers()).thenReturn(3L);
        when(platformUsersPort.findUserIdsAfter(0, 50)).thenReturn(List.of(1, 2, 3));
        when(platformUsersPort.findUserIdsAfter(3, 50)).thenReturn(List.of());
        when(walletTransactionRepository.existsByIdempotencyKey("bulk-job-1-1")).thenReturn(true);
        when(creditWalletUseCase.execute(eq(2L), any(), any(), any(), any(), anyString()))
                .thenThrow(new IllegalStateException("wallet locked"));

        BulkCreditWalletResponse response = useCase.execute(AMOUNT, "Bono", "job-1");

        assertThat(response.getSkipped()).isEqualTo(1);
        assertThat(response.getFailed()).isEqualTo(1);
        assertThat(response.getUsersProcessed()).isEqualTo(1);
        verify(creditWalletUseCase, never()).execute(eq(1L), any(), any(), any(), any(), anyString());
    }

    @Test
    @DisplayName("sin usuarios no acredita nada")
    void noUsers() {
        when(platformUsersPort.countUsers()).thenReturn(0L);
        when(platformUsersPort.findUserIdsAfter(0, 50)).thenReturn(List.of());

        BulkCreditWalletResponse response = useCase.execute(AMOUNT, "Bono", null);

        assertThat(response.getUsersProcessed()).isZero();
        assertThat(response.getJobId()).startsWith("bulk-");
        verify(creditWalletUseCase, never()).execute(any(), any(), any(), any(), any(), anyString());
    }
}
