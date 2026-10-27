package com.capysoft.tuevento.modules.profile.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.profile.infrastructure.persistence.entity.ProfileTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfileTranslationJpaRepository extends JpaRepository<ProfileTranslationEntity, Integer> {

    List<ProfileTranslationEntity> findByProfileId(Integer profileId);

    Optional<ProfileTranslationEntity> findByProfileIdAndLanguageId(Integer profileId, Integer languageId);

    List<ProfileTranslationEntity> findByLanguageId(Integer languageId);
}
