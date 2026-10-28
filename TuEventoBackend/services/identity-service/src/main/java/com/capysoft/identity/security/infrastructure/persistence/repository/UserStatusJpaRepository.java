package com.capysoft.identity.security.infrastructure.persistence.repository;

import com.capysoft.identity.security.infrastructure.persistence.entity.UserStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserStatusJpaRepository extends JpaRepository<UserStatusEntity, Integer> {

    Optional<UserStatusEntity> findByCode(String code);
}
