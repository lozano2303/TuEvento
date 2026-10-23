package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.PasswordHistoryEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PasswordHistoryJpaRepository extends JpaRepository<PasswordHistoryEntity, Integer> {

    @Query("SELECT p FROM PasswordHistoryEntity p WHERE p.user.userId = :userId ORDER BY p.changedAt DESC")
    List<PasswordHistoryEntity> findRecentByUserId(@Param("userId") Integer userId, Pageable pageable);
}
