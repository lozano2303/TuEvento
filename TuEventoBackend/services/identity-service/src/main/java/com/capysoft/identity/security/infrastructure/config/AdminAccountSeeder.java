package com.capysoft.identity.security.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.capysoft.common.domain.exception.NotFoundException;
import com.capysoft.identity.profile.application.dto.request.CreateProfileRequest;
import com.capysoft.identity.profile.application.port.in.CreateProfilePort;
import com.capysoft.identity.profile.application.port.in.GetProfileByUserIdPort;
import com.capysoft.identity.security.application.port.out.PasswordEncoderPort;
import com.capysoft.identity.security.infrastructure.persistence.entity.LoginCredentialsEntity;
import com.capysoft.identity.security.infrastructure.persistence.entity.UserEntity;
import com.capysoft.identity.security.infrastructure.persistence.repository.LoginCredentialsJpaRepository;
import com.capysoft.identity.security.infrastructure.persistence.repository.RoleJpaRepository;
import com.capysoft.identity.security.infrastructure.persistence.repository.UserJpaRepository;
import com.capysoft.identity.security.infrastructure.persistence.repository.UserStatusJpaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Crea al arrancar la cuenta de administrador inicial (y su perfil) si no existe, con
 * {@code app.admin.*}. Es lo que hacía el DataInitializer del monolito: los roles y estados los
 * siembra su Liquibase. Si otra réplica la crea a la vez, la restricción única del email lo
 * resuelve y aquí solo se registra.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.admin.seed", havingValue = "true", matchIfMissing = true)
public class AdminAccountSeeder implements ApplicationRunner {

    static final String ADMIN_FULL_NAME = "Administrador Principal";
    static final int SPANISH_LANGUAGE_ID = 1;

    private final UserJpaRepository userJpaRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final UserStatusJpaRepository userStatusJpaRepository;
    private final LoginCredentialsJpaRepository loginCredentialsJpaRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final CreateProfilePort createProfilePort;
    private final GetProfileByUserIdPort getProfileByUserIdPort;

    @Value("${app.admin.email:}")
    private String adminEmail;
    @Value("${app.admin.password:}")
    private String adminPassword;
    @Value("${app.admin.alias:admin}")
    private String adminAlias;

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.warn("app.admin.email/password not configured: the initial admin account is not created");
            return;
        }
        UserEntity admin;
        try {
            admin = loginCredentialsJpaRepository.findByEmail(adminEmail)
                    .map(LoginCredentialsEntity::getUser)
                    .orElseGet(this::createAdmin);
        } catch (DataIntegrityViolationException e) {
            log.info("Admin account was created concurrently by another instance, skipping");
            return;
        }
        ensureProfile(admin.getUserId());
    }

    private UserEntity createAdmin() {
        log.info("Creating default admin user...");
        var adminRole = roleJpaRepository.findByCode("ADMIN")
                .orElseThrow(() -> new IllegalStateException("ADMIN role not found"));
        var activeStatus = userStatusJpaRepository.findByCode("ACTIVE")
                .orElseThrow(() -> new IllegalStateException("ACTIVE status not found"));
        UserEntity saved = userJpaRepository.save(UserEntity.builder()
                .alias(adminAlias)
                .activated(true)
                .role(adminRole)
                .userStatus(activeStatus)
                .build());
        loginCredentialsJpaRepository.save(LoginCredentialsEntity.builder()
                .user(saved)
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .build());
        log.info("Default admin user created with email: {}", adminEmail);
        return saved;
    }

    private void ensureProfile(Integer userId) {
        try {
            getProfileByUserIdPort.getByUserId(userId);
            return;
        } catch (NotFoundException missing) {
            // se crea abajo
        }
        try {
            createProfilePort.create(CreateProfileRequest.builder()
                    .userId(userId)
                    .fullName(ADMIN_FULL_NAME)
                    .storedFileId(null)
                    .preferredLanguageId(SPANISH_LANGUAGE_ID)
                    .build());
            log.info("Admin profile created for userId={}", userId);
        } catch (Exception e) {
            log.warn("Could not create admin profile: {}", e.getMessage());
        }
    }
}
