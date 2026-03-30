package com.capysoft.tuevento.modules.security.application.usecase;

import com.capysoft.tuevento.modules.security.application.dto.request.ChangePasswordRequest;
import com.capysoft.tuevento.modules.security.application.port.in.ChangePasswordPort;
import com.capysoft.tuevento.modules.security.application.port.out.PasswordEncoderPort;
import com.capysoft.tuevento.modules.security.domain.event.PasswordChangedEvent;
import com.capysoft.tuevento.modules.security.domain.model.LoginCredentials;
import com.capysoft.tuevento.modules.security.domain.model.PasswordHistory;
import com.capysoft.tuevento.modules.security.domain.repository.LoginCredentialsRepository;
import com.capysoft.tuevento.modules.security.domain.repository.PasswordHistoryRepository;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityUser;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChangePasswordUseCase implements ChangePasswordPort {

    private static final int HISTORY_CHECK_LIMIT = 5;

    private final LoginCredentialsRepository loginCredentialsRepository;
    private final PasswordHistoryRepository  passwordHistoryRepository;
    private final PasswordEncoderPort        passwordEncoder;
    private final ApplicationEventPublisher  eventPublisher;

    @Override
    @Transactional
    public void change(ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("PASSWORD_MISMATCH", "Passwords do not match");
        }

        SecurityUser securityUser = (SecurityUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        LoginCredentials credentials = loginCredentialsRepository.findByUserId(securityUser.getUserId())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), credentials.getPasswordHash())) {
            throw new BusinessException("INVALID_CURRENT_PASSWORD", "Current password is incorrect");
        }

        List<PasswordHistory> recentHistory = passwordHistoryRepository
                .findRecentByUserId(credentials.getUser().getUserId(), HISTORY_CHECK_LIMIT);

        boolean reused = recentHistory.stream()
                .anyMatch(h -> passwordEncoder.matches(request.getNewPassword(), h.getPasswordHash()));
        if (reused) {
            throw new BusinessException("PASSWORD_RECENTLY_USED",
                    "New password was recently used. Please choose a different password");
        }

        passwordHistoryRepository.save(PasswordHistory.builder()
                .user(credentials.getUser())
                .passwordHash(credentials.getPasswordHash())
                .changedAt(LocalDateTime.now())
                .build());

        credentials.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        loginCredentialsRepository.save(credentials);

        eventPublisher.publishEvent(PasswordChangedEvent.builder()
                .userId(credentials.getUser().getUserId())
                .occurredAt(LocalDateTime.now())
                .build());
    }
}
