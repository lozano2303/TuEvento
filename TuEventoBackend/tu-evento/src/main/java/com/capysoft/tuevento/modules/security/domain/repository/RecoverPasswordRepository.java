package com.capysoft.tuevento.modules.security.domain.repository;

import com.capysoft.tuevento.modules.security.domain.model.RecoverPassword;

import java.util.Optional;

public interface RecoverPasswordRepository {

    RecoverPassword save(RecoverPassword recoverPassword);
    Optional<RecoverPassword> findActiveByUserId(Integer userId);
    Optional<RecoverPassword> findByCode(String code);
    void invalidateAllByUserId(Integer userId);
}
