package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.AccountLockoutEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountLockoutJpaRepository extends JpaRepository<AccountLockoutEntity, Integer> {

    Optional<AccountLockoutEntity> findByUserUserId(Integer userId);

    @Modifying
    @Query("DELETE FROM AccountLockoutEntity a WHERE a.user.userId = :userId")
    void deleteByUserUserId(@Param("userId") Integer userId);
}
