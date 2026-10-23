package com.capysoft.tuevento.modules.security.application.usecase;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.tuevento.modules.security.application.dto.request.ActivateAccountRequest;
import com.capysoft.tuevento.modules.security.application.port.in.ActivateAccountPort;
import com.capysoft.tuevento.modules.security.domain.event.UserActivatedEvent;
import com.capysoft.tuevento.modules.security.domain.model.AccountActivation;
import com.capysoft.tuevento.modules.security.domain.model.LoginCredentials;
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

    private static final Logger log = LoggerFactory.getLogger(ActivateAccountUseCase.class);
    
    private final AccountActivationRepository accountActivationRepository;
    private final LoginCredentialsRepository  loginCredentialsRepository;
    private final UserRepository              userRepository;
    private final UserStatusRepository        userStatusRepository;
    private final ApplicationEventPublisher   eventPublisher;

    @Override
    @Transactional
    public void activate(ActivateAccountRequest request) {
        // Nunca se registra el código de activación: daría acceso a la cuenta a quien lea los logs
        // 1. Buscar por email
        LoginCredentials credentials = loginCredentialsRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Usuario no encontrado"));

        User user = credentials.getUser();
        log.info("Activating account for user {}", user.getUserId());

        // 2. Verificar si ya está activado
        if (user.getActivated() != null && user.getActivated()) {
            throw new BusinessException("ACCOUNT_ALREADY_ACTIVATED", "La cuenta ya está activada");
        }
        
        // 3. Buscar el código de activación por USUARIO y CÓDIGO
        Optional<AccountActivation> activationOpt = accountActivationRepository
                .findByUserAndActivationCodeAndActivatedFalse(user, request.getActivationCode());

        if (activationOpt.isEmpty()) {
            log.warn("Invalid activation code for user {}", user.getUserId());
            throw new BusinessException("INVALID_ACTIVATION_CODE", "Código de activación inválido");
        }

        AccountActivation activation = activationOpt.get();

        // 4. Verificar si expiró
        if (activation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("ACTIVATION_CODE_EXPIRED", "El código de activación ha expirado");
        }
        
        // 5. Activar
        activation.setActivated(true);
        accountActivationRepository.save(activation);
        
        user.setActivated(true);
        
        // Cambiar el estado a ACTIVE
        UserStatus activeStatus = userStatusRepository.findByCode("ACTIVE")
                .orElseThrow(() -> new NotFoundException("STATUS_NOT_FOUND", "ACTIVE status not found"));
        user.setUserStatus(activeStatus);
        userRepository.save(user);
        
        log.info("Account activated successfully for user: {}", user.getUserId());
        
        // Publicar evento
        eventPublisher.publishEvent(UserActivatedEvent.builder()
                .userId(user.getUserId())
                .alias(user.getAlias())
                .occurredAt(LocalDateTime.now())
                .build());
    }
}