package com.capysoft.tuevento.modules.security.infrastructure;

import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.AccountActivationJpaRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.AuthSessionJpaRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.RecoverPasswordJpaRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityDataCleanupTask {

    private final AuthSessionJpaRepository authSessionJpaRepository;
    private final RefreshTokenJpaRepository refreshTokenJpaRepository;
    private final AccountActivationJpaRepository accountActivationJpaRepository;
    private final RecoverPasswordJpaRepository recoverPasswordJpaRepository;

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
}
