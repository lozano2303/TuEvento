package com.capysoft.tuevento.modules.profile.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.profile.infrastructure.persistence.entity.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfileJpaRepository extends JpaRepository<ProfileEntity, Long> {

    Optional<ProfileEntity> findByUserId(Integer userId);
    boolean existsByUserId(Integer userId);
}
