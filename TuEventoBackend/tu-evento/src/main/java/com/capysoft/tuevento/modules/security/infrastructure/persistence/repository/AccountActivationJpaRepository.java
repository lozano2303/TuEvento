package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.AccountActivationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountActivationJpaRepository extends JpaRepository<AccountActivationEntity, Integer> {

    Optional<AccountActivationEntity> findByUserUserId(Integer userId);
    Optional<AccountActivationEntity> findByActivationCode(String activationCode);

    @Modifying
    @Query("DELETE FROM AccountActivationEntity a WHERE a.user.userId = :userId")
    void deleteByUserUserId(@Param("userId") Integer userId);
}
