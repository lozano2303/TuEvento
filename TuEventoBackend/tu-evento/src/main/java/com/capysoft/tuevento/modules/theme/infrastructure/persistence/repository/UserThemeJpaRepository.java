package com.capysoft.tuevento.modules.theme.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.theme.infrastructure.persistence.entity.UserThemeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserThemeJpaRepository extends JpaRepository<UserThemeEntity, Integer> {

    List<UserThemeEntity> findByUserId(Integer userId);

    Optional<UserThemeEntity> findByUserIdAndIsActiveTrue(Integer userId);

    boolean existsByUserIdAndThemeId(Integer userId, Integer themeId);

    @Modifying
    @Query("UPDATE UserThemeEntity u SET u.isActive = false WHERE u.userId = :userId")
    void updateIsActiveByUserId(@Param("userId") Integer userId);
}
