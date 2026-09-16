package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.AccountReactivationTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountReactivationTokenJpaRepository
        extends JpaRepository<AccountReactivationTokenEntity, Integer> {

    Optional<AccountReactivationTokenEntity> findByToken(String token);

    @Modifying
    @Query("UPDATE AccountReactivationTokenEntity t SET t.tokenStatus = true " +
           "WHERE t.user.userId = :userId AND t.tokenStatus = false")
    void invalidateAllByUserId(@Param("userId") Integer userId);
}
