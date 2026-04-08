package com.capysoft.tuevento.shared.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.LoginCredentialsEntity;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.UserEntity;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.LoginCredentialsJpaRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.RoleJpaRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.UserJpaRepository;
import com.capysoft.tuevento.modules.security.infrastructure.persistence.repository.UserStatusJpaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserJpaRepository userJpaRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final UserStatusJpaRepository userStatusJpaRepository;
    private final LoginCredentialsJpaRepository loginCredentialsJpaRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.alias}")
    private String adminAlias;

    @Override
    public void run(ApplicationArguments args) {
        if (loginCredentialsJpaRepository.existsByEmail(adminEmail)) {
            log.info("Admin user already exists, skipping initialization");
            return;
        }

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

        var savedUser = userJpaRepository.save(adminUser);

        var credentials = LoginCredentialsEntity.builder()
                .user(savedUser)
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .build();

        loginCredentialsJpaRepository.save(credentials);

        log.info("Default admin user created successfully with email: {}", adminEmail);
    }
}
