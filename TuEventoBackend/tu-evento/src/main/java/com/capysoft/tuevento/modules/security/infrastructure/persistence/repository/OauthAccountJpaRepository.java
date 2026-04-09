package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.OauthAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OauthAccountJpaRepository extends JpaRepository<OauthAccountEntity, Integer> {

    Optional<OauthAccountEntity> findByProviderAndProviderUserId(String provider, String providerUserId);
    Optional<OauthAccountEntity> findByUserUserIdAndProvider(Integer userId, String provider);
    List<OauthAccountEntity> findByUserUserId(Integer userId);

    @Modifying
    @Query("DELETE FROM OauthAccountEntity o WHERE o.user.userId = :userId AND o.provider = :provider")
    void deleteByUserUserIdAndProvider(@Param("userId") Integer userId, @Param("provider") String provider);
}
