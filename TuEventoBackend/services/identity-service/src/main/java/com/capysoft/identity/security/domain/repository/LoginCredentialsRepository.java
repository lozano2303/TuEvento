package com.capysoft.identity.security.domain.repository;

import com.capysoft.identity.security.domain.model.LoginCredentials;

import java.util.Optional;

public interface LoginCredentialsRepository {

    LoginCredentials save(LoginCredentials loginCredentials);
    Optional<LoginCredentials> findByEmail(String email);
    Optional<LoginCredentials> findByUserId(Integer userId);
    boolean existsByEmail(String email);
    
    /**
     * Obtiene solo el email de un usuario sin cargar la entidad completa.
     * Útil para contextos async sin sesión Hibernate.
     */
    Optional<String> findEmailByUserId(Integer userId);
}
