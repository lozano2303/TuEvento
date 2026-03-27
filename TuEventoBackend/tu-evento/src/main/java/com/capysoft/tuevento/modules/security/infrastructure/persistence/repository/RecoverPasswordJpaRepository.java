package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.RecoverPasswordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RecoverPasswordJpaRepository extends JpaRepository<RecoverPasswordEntity, Integer> {

    @Query("SELECT r FROM RecoverPasswordEntity r WHERE r.user.userId = :userId AND r.codeStatus = false")
    Optional<RecoverPasswordEntity> findActiveByUserId(@Param("userId") Integer userId);

    Optional<RecoverPasswordEntity> findByCode(String code);

    @Modifying
    @Query("UPDATE RecoverPasswordEntity r SET r.codeStatus = true WHERE r.user.userId = :userId AND r.codeStatus = false")
    void invalidateAllByUserId(@Param("userId") Integer userId);
}
