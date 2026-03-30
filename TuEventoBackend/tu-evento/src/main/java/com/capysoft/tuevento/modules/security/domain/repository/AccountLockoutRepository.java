package com.capysoft.tuevento.modules.security.domain.repository;

import com.capysoft.tuevento.modules.security.domain.model.AccountLockout;

import java.util.Optional;

public interface AccountLockoutRepository {

    AccountLockout save(AccountLockout accountLockout);
    Optional<AccountLockout> findByUserId(Integer userId);
    void deleteByUserId(Integer userId);
}
