package com.capysoft.tuevento.shared.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.profile.application.dto.request.CreateProfileRequest;
import com.capysoft.tuevento.modules.profile.application.port.in.CreateProfilePort;
import com.capysoft.tuevento.modules.profile.application.port.in.GetProfileByUserIdPort;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.LoginCredentialsEntity;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.UserEntity;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.LoginCredentialsJpaRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.RoleJpaRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.UserJpaRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.UserStatusJpaRepository;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final String ADMIN_FULL_NAME = "Tu Evento Admin";

    private final UserJpaRepository             userJpaRepository;
    private final RoleJpaRepository             roleJpaRepository;
    private final UserStatusJpaRepository       userStatusJpaRepository;
    private final LoginCredentialsJpaRepository loginCredentialsJpaRepository;
    private final PasswordEncoder               passwordEncoder;
    private final CreateProfilePort             createProfilePort;
    private final GetProfileByUserIdPort        getProfileByUserIdPort;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.alias}")
    private String adminAlias;

    @Override
    public void run(ApplicationArguments args) {
        UserEntity savedUser;

        if (loginCredentialsJpaRepository.existsByEmail(adminEmail)) {
            log.info("Admin user already exists, skipping user creation");
            savedUser = loginCredentialsJpaRepository.findByEmail(adminEmail)
                    .map(c -> c.getUser())
                    .orElse(null);
        } else {
            log.info("Creating default admin user...");

            var adminRole = roleJpaRepository.findByCode("ADMIN")
                    .orElseThrow(() -> new IllegalStateException("ADMIN role not found"));

            var activeStatus = userStatusJpaRepository.findByCode("ACTIVE")
                    .orElseThrow(() -> new IllegalStateException("ACTIVE status not found"));

            var adminUser = UserEntity.builder()
                    .alias(adminAlias)
                    .activated(true)
                    .role(adminRole)
                    .userStatus(activeStatus)
                    .build();

            savedUser = userJpaRepository.save(adminUser);

            var credentials = LoginCredentialsEntity.builder()
                    .user(savedUser)
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .build();

            loginCredentialsJpaRepository.save(credentials);
            log.info("Default admin user created successfully with email: {}", adminEmail);
        }

        if (savedUser == null) return;

        final Integer userId = savedUser.getUserId();
        boolean profileExists = true;
        try {
            getProfileByUserIdPort.getByUserId(userId);
        } catch (NotFoundException e) {
            profileExists = false;
        }

        if (!profileExists) {
            createProfilePort.create(CreateProfileRequest.builder()
                    .userId(userId)
                    .fullName(ADMIN_FULL_NAME)
                    .storedFileId(null)
                    .build());
            log.info("Admin profile created for userId={}", userId);
        } else {
            log.info("Admin profile already exists, skipping profile creation");
        }
    }
}
