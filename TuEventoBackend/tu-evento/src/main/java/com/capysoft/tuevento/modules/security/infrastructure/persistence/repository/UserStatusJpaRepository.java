package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.UserStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserStatusJpaRepository extends JpaRepository<UserStatusEntity, Integer> {

    Optional<UserStatusEntity> findByCode(String code);
}
