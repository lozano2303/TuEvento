package com.capysoft.identity.security.infrastructure.persistence.repository;

import com.capysoft.identity.security.infrastructure.persistence.entity.LoginCredentialsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LoginCredentialsJpaRepository extends JpaRepository<LoginCredentialsEntity, Integer> {

    @Query("SELECT lc FROM LoginCredentialsEntity lc JOIN FETCH lc.user u JOIN FETCH u.role JOIN FETCH u.userStatus WHERE lc.email = :email")
    Optional<LoginCredentialsEntity> findByEmail(@Param("email") String email);

    @Query("SELECT lc FROM LoginCredentialsEntity lc JOIN FETCH lc.user u JOIN FETCH u.role JOIN FETCH u.userStatus WHERE u.userId = :userId")
    Optional<LoginCredentialsEntity> findByUserUserId(@Param("userId") Integer userId);
    
    /**
     * Obtiene solo el email sin cargar la entidad completa.
     * Retorna directamente el valor del campo email como String.
     */
    @Query("SELECT lc.email FROM LoginCredentialsEntity lc WHERE lc.user.userId = :userId")
    Optional<String> findEmailByUserId(@Param("userId") Integer userId);
    
    boolean existsByEmail(String email);
}
