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
        log.info("=== ACTIVATING ACCOUNT ===");
        log.info("Email: {}", request.getEmail());
        log.info("Activation Code: {}", request.getActivationCode());
        
        // 1. Buscar por email
        LoginCredentials credentials = loginCredentialsRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Usuario no encontrado"));

        User user = credentials.getUser();
        log.info("User found: ID={}, Activated={}", user.getUserId(), user.getActivated());
        
        // 2. Verificar si ya está activado
        if (user.getActivated() != null && user.getActivated()) {
            throw new BusinessException("ACCOUNT_ALREADY_ACTIVATED", "La cuenta ya está activada");
        }
        
        // 3. Buscar el código de activación por USUARIO y CÓDIGO (CORREGIDO)
        log.info("Searching for activation code - UserID: {}, Code: {}", user.getUserId(), request.getActivationCode());

        // Debug: listar todos los códigos del usuario NO ACTIVADOS
        Optional<AccountActivation> allUserCodes = accountActivationRepository.findByUserAndActivatedFalse(user);
        log.info("All non-activated codes for user {}: {}", user.getUserId(),
            allUserCodes.isPresent() ? "FOUND" : "NOT FOUND");
        if (allUserCodes.isPresent()) {
            AccountActivation act = allUserCodes.get();
            log.info("Stored code details - UserID: {}, Activated: {}, Code: {}, ExpiresAt: {}",
                act.getUser().getUserId(), act.getActivated(), act.getActivationCode(), act.getExpiresAt());
        }

        Optional<AccountActivation> activationOpt = accountActivationRepository
                .findByUserAndActivationCodeAndActivatedFalse(user, request.getActivationCode());

        log.info("Activation found: {}", activationOpt.isPresent());

        if (activationOpt.isEmpty()) {
            log.warn("Invalid activation code for user {}: {}", user.getUserId(), request.getActivationCode());
            // Intentar buscar por código solamente para debugging
            Optional<AccountActivation> byCodeOnly = accountActivationRepository.findByActivationCode(request.getActivationCode());
            log.info("Found by code only: {}", byCodeOnly.isPresent());
            if (byCodeOnly.isPresent()) {
                AccountActivation act = byCodeOnly.get();
                log.info("Code details - UserID: {}, Activated: {}, Code: {}",
                    act.getUser().getUserId(), act.getActivated(), act.getActivationCode());
            }
            throw new BusinessException("INVALID_ACTIVATION_CODE", "Código de activación inválido");
        }
        
        AccountActivation activation = activationOpt.get();
        log.info("Activation found: ID={}, ExpiresAt={}", activation.getAccountActivationId(), activation.getExpiresAt());
        
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