package com.capysoft.tuevento.modules.security.infrastructure;

import com.capysoft.tuevento.modules.security.application.usecase.AccountLockoutService;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.AccountActivationJpaRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.AccountLockoutJpaRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.AuthSessionJpaRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.RecoverPasswordJpaRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityDataCleanupTask — desbloqueo de lockouts vencidos")
class SecurityDataCleanupTaskTest {

    @Mock AuthSessionJpaRepository       authSessionJpaRepository;
    @Mock RefreshTokenJpaRepository      refreshTokenJpaRepository;
    @Mock AccountActivationJpaRepository accountActivationJpaRepository;
    @Mock RecoverPasswordJpaRepository   recoverPasswordJpaRepository;
    @Mock AccountLockoutJpaRepository    accountLockoutJpaRepository;
    @Mock AccountLockoutService          accountLockoutService;

    @InjectMocks
    SecurityDataCleanupTask task;

    @Test
    @DisplayName("borra cada lockout vencido")
    void unlocksEveryExpiredLockout() {
        when(accountLockoutJpaRepository.findAllExpiredUserIds(any())).thenReturn(List.of(3, 8));

        task.unlockExpiredLockouts();

        verify(accountLockoutService).deleteLockout(3);
        verify(accountLockoutService).deleteLockout(8);
    }

    @Test
    @DisplayName("sin lockouts vencidos no hace nada")
    void doesNothingWithoutExpiredLockouts() {
        when(accountLockoutJpaRepository.findAllExpiredUserIds(any())).thenReturn(List.of());

        task.unlockExpiredLockouts();

        verify(accountLockoutService, never()).deleteLockout(anyInt());
    }
}
