package com.capysoft.identity.security.domain.repository;

import com.capysoft.identity.security.domain.model.AccountReactivationToken;

import java.util.Optional;

public interface AccountReactivationTokenRepository {

    AccountReactivationToken save(AccountReactivationToken token);
    Optional<AccountReactivationToken> findByToken(String token);
    void invalidateAllByUserId(Integer userId);
}
