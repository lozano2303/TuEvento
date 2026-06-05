package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.LoginCredentialsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LoginCredentialsJpaRepository extends JpaRepository<LoginCredentialsEntity, Integer> {

    @Query("SELECT lc FROM LoginCredentialsEntity lc JOIN FETCH lc.user u JOIN FETCH u.role JOIN FETCH u.userStatus WHERE lc.email = :email")
    Optional<LoginCredentialsEntity> findByEmail(@Param("email") String email);

    Optional<LoginCredentialsEntity> findByUserUserId(Integer userId);
    boolean existsByEmail(String email);
}
