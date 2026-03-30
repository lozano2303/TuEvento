package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.LoginCredentialsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoginCredentialsJpaRepository extends JpaRepository<LoginCredentialsEntity, Integer> {

    Optional<LoginCredentialsEntity> findByEmail(String email);
    Optional<LoginCredentialsEntity> findByUserUserId(Integer userId);
    boolean existsByEmail(String email);
}
