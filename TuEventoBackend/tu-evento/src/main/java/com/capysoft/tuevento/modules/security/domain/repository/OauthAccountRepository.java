package com.capysoft.tuevento.modules.security.domain.repository;

import com.capysoft.tuevento.modules.security.domain.model.OauthAccount;

import java.util.List;
import java.util.Optional;

public interface OauthAccountRepository {

    OauthAccount save(OauthAccount oauthAccount);
    Optional<OauthAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
    Optional<OauthAccount> findByUserIdAndProvider(Integer userId, String provider);
    List<OauthAccount> findByUserId(Integer userId);
    void deleteByUserIdAndProvider(Integer userId, String provider);
}
