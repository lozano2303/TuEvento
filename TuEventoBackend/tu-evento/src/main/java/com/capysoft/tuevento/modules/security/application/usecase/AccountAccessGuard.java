package com.capysoft.tuevento.modules.security.application.usecase;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.security.domain.model.User;
import com.capysoft.tuevento.modules.security.domain.repository.AccountLockoutRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * Account checks for social sign-in (Google, Facebook), with the same rules and error codes as
 * LoginUseCase: a temporary lockout still in force rejects the sign-in, an expired one is lifted
 * (AccountLockoutService restores ACTIVE), and BLOCKED / INACTIVE / DELETED accounts are rejected.
 */
@Component
@RequiredArgsConstructor
public class AccountAccessGuard {

    private final AccountLockoutRepository accountLockoutRepository;
    private final AccountLockoutService    accountLockoutService;

    public void assertCanSignIn(User user) {
        boolean lockoutLifted = liftExpiredLockout(user);

        String statusCode = user.getUserStatus().getCode();
        // deleteLockout restores ACTIVE in its own transaction, so the status already loaded
        // in this one may still say BLOCKED.
        if ("BLOCKED".equals(statusCode) && !lockoutLifted) {
            throw new BusinessException("ACCOUNT_BLOCKED",
                    "Tu cuenta ha sido bloqueada. Por favor contacta a soporte.");
        }
        if ("INACTIVE".equals(statusCode)) {
            throw new BusinessException("ACCOUNT_DEACTIVATED",
                    "Tu cuenta ha sido desactivada. Solicita la reactivación para que nuestro equipo la revise.");
        }
        if ("DELETED".equals(statusCode)) {
            throw new BusinessException("ACCOUNT_DELETED", "Cuenta no encontrada.");
        }
    }

    /** Throws ACCOUNT_LOCKED while the lockout is in force; returns true when an expired one was lifted. */
    private boolean liftExpiredLockout(User user) {
        return accountLockoutRepository.findByUserId(user.getUserId())
                .filter(lockout -> lockout.getLockedUntil() != null)
                .map(lockout -> {
                    if (lockout.getLockedUntil().isAfter(LocalDateTime.now())) {
                        throw new BusinessException("ACCOUNT_LOCKED",
                                "Cuenta bloqueada temporalmente. Por favor intenta de nuevo más tarde.");
                    }
                    accountLockoutService.deleteLockout(user.getUserId());
                    return true;
                })
                .orElse(false);
    }
}
