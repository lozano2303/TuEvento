package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.UserEntity;

public interface UserJpaRepository extends JpaRepository<UserEntity, Integer> {

    Optional<UserEntity> findByAlias(String alias);
    boolean existsByAlias(String alias);

    @Modifying
    @Query("UPDATE UserEntity u SET u.userStatus = (SELECT s FROM UserStatusEntity s WHERE s.code = :statusCode) WHERE u.userId = :userId")
    void updateStatusByUserId(@Param("userId") Integer userId, @Param("statusCode") String statusCode);

    @Query("SELECT u.userStatus.code FROM UserEntity u WHERE u.userId = :userId")
    String findStatusCodeByUserId(@Param("userId") Integer userId);
}
