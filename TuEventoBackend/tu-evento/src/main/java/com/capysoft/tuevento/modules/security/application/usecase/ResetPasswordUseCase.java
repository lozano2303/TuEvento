package com.capysoft.tuevento.modules.security.application.usecase;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.tuevento.modules.security.application.dto.request.ResetPasswordRequest;
import com.capysoft.tuevento.modules.security.application.port.in.ResetPasswordPort;
import com.capysoft.tuevento.modules.security.application.port.out.PasswordEncoderPort;
import com.capysoft.tuevento.modules.security.domain.event.PasswordChangedEvent;
import com.capysoft.tuevento.modules.security.domain.model.LoginCredentials;
import com.capysoft.tuevento.modules.security.domain.model.PasswordHistory;
import com.capysoft.tuevento.modules.security.domain.model.RecoverPassword;
import com.capysoft.tuevento.modules.security.domain.repository.LoginCredentialsRepository;
import com.capysoft.tuevento.modules.security.domain.repository.PasswordHistoryRepository;
import com.capysoft.tuevento.modules.security.domain.repository.RecoverPasswordRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResetPasswordUseCase implements ResetPasswordPort {

    private final LoginCredentialsRepository loginCredentialsRepository;
    private final RecoverPasswordRepository  recoverPasswordRepository;
    private final PasswordHistoryRepository  passwordHistoryRepository;
    private final PasswordEncoderPort        passwordEncoder;
    private final ApplicationEventPublisher  eventPublisher;

    @Override
    @Transactional
    public void reset(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("PASSWORD_MISMATCH", "Passwords do not match");
        }

        LoginCredentials credentials = loginCredentialsRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));

        RecoverPassword recovery = recoverPasswordRepository.findByCode(request.getCode())
                .orElseThrow(() -> new NotFoundException("RECOVERY_CODE_NOT_FOUND", "Invalid recovery code"));

        // Verify the recovery code belongs to the user identified by the email
        if (!recovery.getUser().getUserId().equals(credentials.getUser().getUserId())) {
            throw new BusinessException("INVALID_RECOVERY_CODE", "Invalid recovery code");
        }

        if (recovery.getCodeStatus()) {
            throw new BusinessException("RECOVERY_CODE_USED", "Recovery code has already been used");
        }
        if (recovery.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("RECOVERY_CODE_EXPIRED", "Recovery code has expired");
        }

        String newHash = passwordEncoder.encode(request.getNewPassword());

        passwordHistoryRepository.save(PasswordHistory.builder()
                .user(credentials.getUser())
                .passwordHash(credentials.getPasswordHash())
                .changedAt(LocalDateTime.now())
                .build());

        credentials.setPasswordHash(newHash);
        loginCredentialsRepository.save(credentials);

        recovery.setCodeStatus(true);
        recovery.setLastPasswordHash(newHash);
        recoverPasswordRepository.save(recovery);

        eventPublisher.publishEvent(PasswordChangedEvent.builder()
                .userId(credentials.getUser().getUserId())
                .occurredAt(LocalDateTime.now())
                .build());
    }
}
