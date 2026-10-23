package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.AccountLockoutEntity;

public interface AccountLockoutJpaRepository extends JpaRepository<AccountLockoutEntity, Integer> {

    Optional<AccountLockoutEntity> findByUserUserId(Integer userId);

    @Modifying
    @Query("DELETE FROM AccountLockoutEntity a WHERE a.user.userId = :userId")
    void deleteByUserUserId(@Param("userId") Integer userId);

    @Query("SELECT a.user.userId FROM AccountLockoutEntity a WHERE a.lockedUntil IS NOT NULL AND a.lockedUntil <= :now")
    List<Integer> findAllExpiredUserIds(@Param("now") LocalDateTime now);
}
