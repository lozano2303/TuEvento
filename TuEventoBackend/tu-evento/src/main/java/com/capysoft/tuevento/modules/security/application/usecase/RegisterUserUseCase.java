package com.capysoft.tuevento.modules.security.application.usecase;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capysoft.tuevento.modules.profile.application.dto.request.CreateProfileRequest;
import com.capysoft.tuevento.modules.profile.application.port.in.CreateProfilePort;
import com.capysoft.tuevento.modules.security.application.dto.request.RegisterUserRequest;
import com.capysoft.tuevento.modules.security.application.dto.response.RegisterUserResponse;
import com.capysoft.tuevento.modules.security.application.port.in.RegisterUserPort;
import com.capysoft.tuevento.modules.security.application.port.out.CodeGeneratorPort;
import com.capysoft.tuevento.modules.security.application.port.out.EmailNotificationPort;
import com.capysoft.tuevento.modules.security.application.port.out.PasswordEncoderPort;
import com.capysoft.tuevento.modules.security.domain.event.UserRegisteredEvent;
import com.capysoft.tuevento.modules.security.domain.model.AccountActivation;
import com.capysoft.tuevento.modules.security.domain.model.LoginCredentials;
import com.capysoft.tuevento.modules.security.domain.model.Role;
import com.capysoft.tuevento.modules.security.domain.model.User;
import com.capysoft.tuevento.modules.security.domain.model.UserStatus;
import com.capysoft.tuevento.modules.security.domain.repository.AccountActivationRepository;
import com.capysoft.tuevento.modules.security.domain.repository.LoginCredentialsRepository;
import com.capysoft.tuevento.modules.security.domain.repository.RoleRepository;
import com.capysoft.tuevento.modules.security.domain.repository.UserRepository;
import com.capysoft.tuevento.modules.security.domain.repository.UserStatusRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import com.capysoft.tuevento.shared.domain.valueobject.AliasGenerator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegisterUserUseCase implements RegisterUserPort {

    private static final String DEFAULT_ROLE_CODE       = "USER";
    private static final String DEFAULT_STATUS_CODE     = "PENDING";
    private static final int    ACTIVATION_EXPIRY_HOURS = 24;

    private final UserRepository              userRepository;
    private final LoginCredentialsRepository  loginCredentialsRepository;
    private final AccountActivationRepository accountActivationRepository;
    private final RoleRepository              roleRepository;
    private final UserStatusRepository        userStatusRepository;
    private final PasswordEncoderPort         passwordEncoder;
    private final CodeGeneratorPort           codeGenerator;
    private final EmailNotificationPort       emailNotification;
    private final CreateProfilePort           createProfilePort;
    private final ApplicationEventPublisher   eventPublisher;

    @Override
    @Transactional
    public RegisterUserResponse register(RegisterUserRequest request) {
        if (loginCredentialsRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("EMAIL_ALREADY_EXISTS", "Email is already registered");
        }

        Role role = roleRepository.findByCode(DEFAULT_ROLE_CODE)
                .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Default role not found"));

        UserStatus status = userStatusRepository.findByCode(DEFAULT_STATUS_CODE)
                .orElseThrow(() -> new NotFoundException("STATUS_NOT_FOUND", "Default user status not found"));

        String alias = AliasGenerator.generateUnique(request.getEmail(), userRepository::existsByAlias);

        User user = userRepository.save(User.builder()
                .userStatus(status)
                .role(role)
                .alias(alias)
                .activated(false)
                .build());

        loginCredentialsRepository.save(LoginCredentials.builder()
                .user(user)
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build());

        // Profile creation is atomic with registration — if it fails the whole transaction rolls back
        createProfilePort.create(CreateProfileRequest.builder()
                .userId(user.getUserId())
                .fullName(request.getFullName())
                .build());

        String code = codeGenerator.generateActivationCode();
        accountActivationRepository.save(AccountActivation.builder()
                .user(user)
                .activationCode(code)
                .activated(false)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(ACTIVATION_EXPIRY_HOURS))
                .build());

        emailNotification.sendActivationEmail(request.getEmail(), alias, code);

        eventPublisher.publishEvent(UserRegisteredEvent.builder()
                .userId(user.getUserId())
                .alias(alias)
                .email(request.getEmail())
                .occurredAt(LocalDateTime.now())
                .build());

        return RegisterUserResponse.builder()
                .userId(user.getUserId())
                .alias(alias)
                .email(request.getEmail())
                .build();
    }
}
