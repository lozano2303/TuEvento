package com.capysoft.tuevento.modules.security.domain.repository;

import com.capysoft.tuevento.modules.security.domain.model.LoginCredentials;

import java.util.Optional;

public interface LoginCredentialsRepository {

    LoginCredentials save(LoginCredentials loginCredentials);
    Optional<LoginCredentials> findByEmail(String email);
    Optional<LoginCredentials> findByUserId(Integer userId);
    boolean existsByEmail(String email);
}
