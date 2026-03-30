package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.application.mapper.OauthAccountMapper;
import com.capysoft.tuevento.modules.security.domain.model.OauthAccount;
import com.capysoft.tuevento.modules.security.domain.repository.OauthAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OauthAccountRepositoryImpl implements OauthAccountRepository {

    private final OauthAccountJpaRepository jpaRepository;
    private final OauthAccountMapper mapper;

    @Override
    public OauthAccount save(OauthAccount oauthAccount) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(oauthAccount)));
    }

    @Override
    public Optional<OauthAccount> findByProviderAndProviderUserId(String provider, String providerUserId) {
        return jpaRepository.findByProviderAndProviderUserId(provider, providerUserId).map(mapper::toDomain);
    }

    @Override
    public Optional<OauthAccount> findByUserIdAndProvider(Integer userId, String provider) {
        return jpaRepository.findByUserUserIdAndProvider(userId, provider).map(mapper::toDomain);
    }

    @Override
    public List<OauthAccount> findByUserId(Integer userId) {
        return jpaRepository.findByUserUserId(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteByUserIdAndProvider(Integer userId, String provider) {
        jpaRepository.deleteByUserUserIdAndProvider(userId, provider);
    }
}
