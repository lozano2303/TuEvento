package com.capysoft.tuevento.modules.security.application.usecase;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.tuevento.modules.security.application.dto.request.LoginRequest;
import com.capysoft.tuevento.modules.security.application.dto.response.LoginResponse;
import com.capysoft.tuevento.modules.security.application.port.in.LoginPort;
import com.capysoft.tuevento.modules.security.application.port.out.PasswordEncoderPort;
import com.capysoft.tuevento.modules.security.application.port.out.TokenGeneratorPort;
import com.capysoft.tuevento.modules.security.domain.event.UserLockedEvent;
import com.capysoft.tuevento.modules.security.domain.model.AccountLockout;
import com.capysoft.tuevento.modules.security.domain.model.AuthSession;
import com.capysoft.tuevento.modules.security.domain.model.LoginCredentials;
import com.capysoft.tuevento.modules.security.domain.model.RefreshToken;
import com.capysoft.tuevento.modules.security.domain.model.User;
import com.capysoft.tuevento.modules.security.domain.repository.AccountLockoutRepository;
import com.capysoft.tuevento.modules.security.domain.repository.AuthSessionRepository;
import com.capysoft.tuevento.modules.security.domain.repository.LoginCredentialsRepository;
import com.capysoft.tuevento.modules.security.domain.repository.RefreshTokenRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginUseCase implements LoginPort {

    private static final int    MAX_FAILED_ATTEMPTS  = 5;
    private static final int    LOCKOUT_MINUTES       = 30;
    private static final int    WINDOW_MINUTES        = 15;
    private static final int    ACCESS_TOKEN_MINUTES  = 15;
    private static final int    REFRESH_TOKEN_DAYS    = 7;

    private final LoginCredentialsRepository loginCredentialsRepository;
    private final AccountLockoutRepository   accountLockoutRepository;
    private final AccountLockoutService      accountLockoutService;
    private final AuthSessionRepository      authSessionRepository;
    private final RefreshTokenRepository     refreshTokenRepository;
    private final PasswordEncoderPort        passwordEncoder;
    private final TokenGeneratorPort         tokenGenerator;
    private final ApplicationEventPublisher  eventPublisher;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        LoginCredentials credentials = loginCredentialsRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Invalid email or password"));

        User user = credentials.getUser();

        if (!user.getActivated()) {
            throw new BusinessException("ACCOUNT_NOT_ACTIVATED", "Account is not activated");
        }

        // Verify user status for blocked/inactive/deleted accounts
        String statusCode = user.getUserStatus().getCode();
        if ("BLOCKED".equals(statusCode)) {
            throw new BusinessException("ACCOUNT_BLOCKED", "Your account has been blocked. Please contact support");
        }
        if ("INACTIVE".equals(statusCode)) {
            throw new BusinessException("ACCOUNT_INACTIVE", "Your account is inactive");
        }
        if ("DELETED".equals(statusCode)) {
            throw new BusinessException("ACCOUNT_DELETED", "Account not found");
        }

        checkLockout(user);

        if (!passwordEncoder.matches(request.getPassword(), credentials.getPasswordHash())) {
            handleFailedAttempt(user);
            throw new BusinessException("INVALID_CREDENTIALS", "Invalid email or password");
        }

        resetLockout(user);

        String accessToken  = tokenGenerator.generateAccessToken(
                user.getUserId(), user.getAlias(), user.getRole().getCode());
        String refreshToken = tokenGenerator.generateRefreshToken(user.getUserId());

        LocalDateTime now = LocalDateTime.now();

        AuthSession session = authSessionRepository.save(AuthSession.builder()
                .user(user)
                .accessToken(accessToken)
                .issuedAt(now)
                .expiresAt(now.plusMinutes(ACCESS_TOKEN_MINUTES))
                .revoked(false)
                .build());

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .authSession(session)
                .token(refreshToken)
                .issuedAt(now)
                .expiresAt(now.plusDays(REFRESH_TOKEN_DAYS))
                .revoked(false)
                .build());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getUserId())
                .alias(user.getAlias())
                .build();
    }

    private void checkLockout(User user) {
        accountLockoutRepository.findByUserId(user.getUserId()).ifPresent(lockout -> {
            if (lockout.getLockedUntil() != null) {
                if (lockout.getLockedUntil().isAfter(LocalDateTime.now())) {
                    throw new BusinessException("ACCOUNT_LOCKED",
                            "Account is temporarily locked. Please try again later");
                }
                // Lockout window expired — auto-unblock
                accountLockoutService.deleteLockout(user.getUserId());
            }
        });
    }

    private void handleFailedAttempt(User user) {
        LocalDateTime now = LocalDateTime.now();
        AccountLockout lockout = accountLockoutRepository.findByUserId(user.getUserId())
                .orElse(AccountLockout.builder()
                        .user(user)
                        .failedAttempts(0)
                        .windowStartedAt(now)
                        .build());

        if (lockout.getWindowStartedAt() == null ||
                lockout.getWindowStartedAt().plusMinutes(WINDOW_MINUTES).isBefore(now)) {
            lockout.setFailedAttempts(1);
            lockout.setWindowStartedAt(now);
            lockout.setLockedUntil(null);
        } else {
            lockout.setFailedAttempts(lockout.getFailedAttempts() + 1);
        }

        if (lockout.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            LocalDateTime lockedUntil = now.plusMinutes(LOCKOUT_MINUTES);
            lockout.setLockedUntil(lockedUntil);
            accountLockoutService.saveLockout(lockout);
            eventPublisher.publishEvent(UserLockedEvent.builder()
                    .userId(user.getUserId())
                    .alias(user.getAlias())
                    .lockedUntil(lockedUntil)
                    .occurredAt(now)
                    .build());
            throw new BusinessException("ACCOUNT_LOCKED", "Account locked due to too many failed attempts");
        }

        accountLockoutService.saveLockout(lockout);
    }

    private void resetLockout(User user) {
        accountLockoutRepository.findByUserId(user.getUserId())
                .ifPresent(lockout -> accountLockoutService.deleteLockout(user.getUserId()));
    }
}
