package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.AuthSessionEntity;

public interface AuthSessionJpaRepository extends JpaRepository<AuthSessionEntity, Integer> {

    Optional<AuthSessionEntity> findByAccessToken(String accessToken);

    @Query("SELECT s FROM AuthSessionEntity s WHERE s.user.userId = :userId AND s.revoked = false")
    List<AuthSessionEntity> findActiveByUserId(@Param("userId") Integer userId);

    @Modifying
    @Query("UPDATE AuthSessionEntity s SET s.revoked = true, s.revokedAt = :now WHERE s.user.userId = :userId AND s.revoked = false")
    void revokeAllByUserId(@Param("userId") Integer userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM AuthSessionEntity a WHERE a.expiresAt < :now OR a.revoked = true")
    void deleteAllExpiredOrRevoked(@Param("now") LocalDateTime now);
}
