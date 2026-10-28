package com.capysoft.identity.security.application.usecase;

import com.capysoft.identity.security.domain.model.AccountLockout;
import com.capysoft.identity.security.domain.model.User;
import com.capysoft.identity.security.domain.model.UserStatus;
import com.capysoft.identity.security.domain.repository.AccountLockoutRepository;
import com.capysoft.common.domain.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Social sign-in applies the same account rules and codes as LoginUseCase.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountAccessGuard")
class AccountAccessGuardTest {

    private static final Integer USER_ID = 7;

    @Mock private AccountLockoutRepository accountLockoutRepository;
    @Mock private AccountLockoutService    accountLockoutService;

    @InjectMocks
    private AccountAccessGuard guard;

    private static User user(String statusCode) {
        return User.builder()
                .userId(USER_ID)
                .userStatus(UserStatus.builder().code(statusCode).build())
                .build();
    }

    private void givenLockoutUntil(LocalDateTime lockedUntil) {
        when(accountLockoutRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(AccountLockout.builder().lockedUntil(lockedUntil).build()));
    }

    private void assertRejected(User user, String code) {
        assertThatThrownBy(() -> guard.assertCanSignIn(user))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", code);
    }

    @Test
    @DisplayName("ACTIVE without lockout → allowed")
    void active_allowed() {
        assertThatCode(() -> guard.assertCanSignIn(user("ACTIVE"))).doesNotThrowAnyException();
        verify(accountLockoutService, never()).deleteLockout(any());
    }

    @Test
    @DisplayName("lockout still in force → ACCOUNT_LOCKED, nothing lifted")
    void lockoutInForce_locked() {
        givenLockoutUntil(LocalDateTime.now().plusMinutes(5));

        assertRejected(user("BLOCKED"), "ACCOUNT_LOCKED");
        verify(accountLockoutService, never()).deleteLockout(any());
    }

    @Test
    @DisplayName("expired lockout → lifted and allowed, even if the loaded status still says BLOCKED")
    void expiredLockout_liftedAndAllowed() {
        givenLockoutUntil(LocalDateTime.now().minusMinutes(1));

        assertThatCode(() -> guard.assertCanSignIn(user("BLOCKED"))).doesNotThrowAnyException();
        verify(accountLockoutService).deleteLockout(USER_ID);
    }

    @Test
    @DisplayName("BLOCKED without a lockout (blocked by an admin) → ACCOUNT_BLOCKED")
    void blocked_rejected() {
        assertRejected(user("BLOCKED"), "ACCOUNT_BLOCKED");
    }

    @Test
    @DisplayName("lockout row without lockedUntil (failed attempts only) → status decides")
    void lockoutWithoutLockedUntil_ignored() {
        givenLockoutUntil(null);

        assertThatCode(() -> guard.assertCanSignIn(user("ACTIVE"))).doesNotThrowAnyException();
        assertRejected(user("BLOCKED"), "ACCOUNT_BLOCKED");
        verify(accountLockoutService, never()).deleteLockout(any());
    }

    @Test
    @DisplayName("INACTIVE → ACCOUNT_DEACTIVATED")
    void inactive_rejected() {
        assertRejected(user("INACTIVE"), "ACCOUNT_DEACTIVATED");
    }

    @Test
    @DisplayName("DELETED → ACCOUNT_DELETED")
    void deleted_rejected() {
        assertRejected(user("DELETED"), "ACCOUNT_DELETED");
    }
}
