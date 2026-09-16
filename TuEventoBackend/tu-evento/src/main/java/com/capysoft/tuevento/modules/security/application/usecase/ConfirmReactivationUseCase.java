package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.security.application.dto.request.ConfirmReactivationRequest;
import com.capysoft.tuevento.modules.security.application.port.in.ConfirmReactivationPort;
import com.capysoft.tuevento.modules.security.domain.model.AccountReactivationToken;
import com.capysoft.tuevento.modules.security.domain.model.User;
import com.capysoft.tuevento.modules.security.domain.model.UserStatus;
import com.capysoft.tuevento.modules.security.domain.model.UserStatusHistory;
import com.capysoft.tuevento.modules.security.domain.repository.AccountReactivationTokenRepository;
import com.capysoft.tuevento.modules.security.domain.repository.UserRepository;
import com.capysoft.tuevento.modules.security.domain.repository.UserStatusHistoryRepository;
import com.capysoft.tuevento.modules.security.domain.repository.UserStatusRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Validates a reactivation token and restores the account to ACTIVE status.
 *
 * Mirrors the structure of ResetPasswordUseCase — same validation chain
 * (existence → ownership implicit via token → used → expired) and
 * the same UserStatusHistory audit trail as DeactivateAccountUseCase.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmReactivationUseCase implements ConfirmReactivationPort {

    private static final String ACTIVE_STATUS_CODE         = "ACTIVE";
    private static final String REACTIVATION_AUDIT_REASON  = "User confirmed account reactivation via email token";

    private final AccountReactivationTokenRepository reactivationTokenRepository;
    private final UserRepository                     userRepository;
    private final UserStatusRepository               userStatusRepository;
    private final UserStatusHistoryRepository        userStatusHistoryRepository;

    @Override
    @Transactional
    public void confirm(ConfirmReactivationRequest request) {
        AccountReactivationToken reactivationToken = reactivationTokenRepository
                .findByToken(request.getToken())
                .orElseThrow(() -> new NotFoundException(
                        "REACTIVATION_TOKEN_NOT_FOUND", "Token de reactivación inválido."));

        if (Boolean.TRUE.equals(reactivationToken.getTokenStatus())) {
            throw new BusinessException(
                    "REACTIVATION_TOKEN_USED", "Este token ya fue utilizado.");
        }

        if (reactivationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(
                    "REACTIVATION_TOKEN_EXPIRED", "El token de reactivación ha expirado. Solicita uno nuevo.");
        }

        UserStatus activeStatus = userStatusRepository.findByCode(ACTIVE_STATUS_CODE)
                .orElseThrow(() -> new NotFoundException(
                        "STATUS_NOT_FOUND", "Estado ACTIVE no encontrado."));

        User user = reactivationToken.getUser();
        user.setUserStatus(activeStatus);
        userRepository.save(user);

        // Audit trail — mirrors DeactivateAccountUseCase
        userStatusHistoryRepository.save(UserStatusHistory.builder()
                .user(user)
                .userStatus(activeStatus)
                .reason(REACTIVATION_AUDIT_REASON)
                .build());

        // Mark token as used (single-use enforcement)
        reactivationToken.setTokenStatus(true);
        reactivationTokenRepository.save(reactivationToken);

        log.info("Account reactivated for userId={}", user.getUserId());
    }
}
