package com.capysoft.tuevento.modules.security.application.usecase;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.tuevento.modules.security.application.dto.request.ActivateAccountRequest;
import com.capysoft.tuevento.modules.security.application.port.in.ActivateAccountPort;
import com.capysoft.tuevento.modules.security.domain.event.UserActivatedEvent;
import com.capysoft.tuevento.modules.security.domain.model.AccountActivation;
import com.capysoft.tuevento.modules.security.domain.model.User;
import com.capysoft.tuevento.modules.security.domain.model.UserStatus;
import com.capysoft.tuevento.modules.security.domain.repository.AccountActivationRepository;
import com.capysoft.tuevento.modules.security.domain.repository.LoginCredentialsRepository;
import com.capysoft.tuevento.modules.security.domain.repository.UserRepository;
import com.capysoft.tuevento.modules.security.domain.repository.UserStatusRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ActivateAccountUseCase implements ActivateAccountPort {

    private final AccountActivationRepository accountActivationRepository;
    private final LoginCredentialsRepository  loginCredentialsRepository;
    private final UserRepository              userRepository;
    private final UserStatusRepository        userStatusRepository;
    private final ApplicationEventPublisher   eventPublisher;

    @Override
    @Transactional
    public void activate(ActivateAccountRequest request) {
        loginCredentialsRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));

        AccountActivation activation = accountActivationRepository
                .findByActivationCode(request.getActivationCode())
                .orElseThrow(() -> new NotFoundException("ACTIVATION_CODE_NOT_FOUND", "Invalid activation code"));

        if (activation.getActivated()) {
            throw new BusinessException("ACCOUNT_ALREADY_ACTIVATED", "Account is already activated");
        }
        if (activation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("ACTIVATION_CODE_EXPIRED", "Activation code has expired");
        }

        activation.setActivated(true);
        accountActivationRepository.save(activation);

        User user = activation.getUser();
        user.setActivated(true);

        UserStatus activeStatus = userStatusRepository.findByCode("ACTIVE")
                .orElseThrow(() -> new NotFoundException("STATUS_NOT_FOUND", "ACTIVE status not found"));
        user.setUserStatus(activeStatus);

        userRepository.save(user);

        eventPublisher.publishEvent(UserActivatedEvent.builder()
                .userId(user.getUserId())
                .alias(user.getAlias())
                .occurredAt(LocalDateTime.now())
                .build());
    }
}
