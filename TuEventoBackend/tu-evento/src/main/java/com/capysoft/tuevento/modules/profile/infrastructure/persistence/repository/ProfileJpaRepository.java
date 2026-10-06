package com.capysoft.tuevento.modules.profile.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.profile.infrastructure.persistence.entity.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public interface ProfileJpaRepository extends JpaRepository<ProfileEntity, Long> {

    Optional<ProfileEntity> findByUserId(Integer userId);
    boolean existsByUserId(Integer userId);
    List<ProfileEntity> findAllByStoredFileIdIsNull();
    List<ProfileEntity> findAllByUserIdIn(List<Integer> userIds);
}
