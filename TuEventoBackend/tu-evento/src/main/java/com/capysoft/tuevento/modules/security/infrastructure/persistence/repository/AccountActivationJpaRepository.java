package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.AccountActivationEntity;

public interface AccountActivationJpaRepository extends JpaRepository<AccountActivationEntity, Integer> {

    Optional<AccountActivationEntity> findByUserUserId(Integer userId);
    Optional<AccountActivationEntity> findByActivationCode(String activationCode);

    @Modifying
    @Query("DELETE FROM AccountActivationEntity a WHERE a.user.userId = :userId")
    void deleteByUserUserId(@Param("userId") Integer userId);

    @Modifying
    @Query("DELETE FROM AccountActivationEntity a WHERE a.expiresAt < :now OR a.activated = true")
    void deleteAllExpiredOrUsed(@Param("now") LocalDateTime now);
}
