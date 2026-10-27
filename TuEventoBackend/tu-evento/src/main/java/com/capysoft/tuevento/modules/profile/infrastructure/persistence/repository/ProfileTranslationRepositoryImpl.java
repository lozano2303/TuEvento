package com.capysoft.tuevento.modules.profile.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.profile.domain.model.ProfileTranslation;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileTranslationRepository;
import com.capysoft.tuevento.modules.profile.infrastructure.persistence.mapper.ProfileTranslationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProfileTranslationRepositoryImpl implements ProfileTranslationRepository {

    private final ProfileTranslationJpaRepository jpaRepository;
    private final ProfileTranslationMapper mapper;

    @Override
    public Optional<ProfileTranslation> findByProfileAndLanguage(Long profileId, Integer languageId) {
        return jpaRepository.findByProfileIdAndLanguageId(profileId.intValue(), languageId).map(mapper::toDomain);
    }

    @Override
    public List<ProfileTranslation> findByLanguage(Integer languageId) {
        return jpaRepository.findByLanguageId(languageId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public ProfileTranslation save(ProfileTranslation translation) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(translation)));
    }

    @Override
    public void delete(ProfileTranslation translation) {
        jpaRepository.delete(mapper.toEntity(translation));
    }

    @Override
    public void deleteByProfile(Long profileId) {
        jpaRepository.deleteAll(jpaRepository.findByProfileId(profileId.intValue()));
    }
}
