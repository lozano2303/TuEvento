package com.capysoft.tuevento.modules.security.infrastructure;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.tuevento.modules.security.application.usecase.AccountLockoutService;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.AccountActivationJpaRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.AccountLockoutJpaRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.AuthSessionJpaRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.RecoverPasswordJpaRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.RefreshTokenJpaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityDataCleanupTask {

    private final AuthSessionJpaRepository authSessionJpaRepository;
    private final RefreshTokenJpaRepository refreshTokenJpaRepository;
    private final AccountActivationJpaRepository accountActivationJpaRepository;
    private final RecoverPasswordJpaRepository recoverPasswordJpaRepository;
    private final AccountLockoutJpaRepository accountLockoutJpaRepository;
    private final AccountLockoutService accountLockoutService;

    @Transactional
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanExpiredData() {
        LocalDateTime now = LocalDateTime.now();
        log.info("Starting security data cleanup task...");

        authSessionJpaRepository.deleteAllExpiredOrRevoked(now);
        log.info("Expired and revoked auth sessions cleaned");

        refreshTokenJpaRepository.deleteAllExpiredOrRevoked(now);
        log.info("Expired and revoked refresh tokens cleaned");

        accountActivationJpaRepository.deleteAllExpiredOrUsed(now);
        log.info("Expired and used account activation codes cleaned");

        recoverPasswordJpaRepository.deleteAllExpiredOrUsed(now);
        log.info("Expired and used password recovery codes cleaned");

        log.info("Security data cleanup task completed successfully");
    }

    // TODO: change to "0 0/30 * * * *" (every 30 min) for production
    @Scheduled(cron = "0 * * * * *")
    public void unlockExpiredLockouts() {
        LocalDateTime now = LocalDateTime.now();
        log.info("[LockoutSchedule] Running at {}", now);
        var expiredUserIds = accountLockoutJpaRepository.findAllExpiredUserIds(now);
        if (expiredUserIds.isEmpty()) return;

        log.info("Unlocking {} expired account lockout(s)...", expiredUserIds.size());
        expiredUserIds.forEach(accountLockoutService::deleteLockout);
        log.info("Expired lockouts processed");
    }
}
