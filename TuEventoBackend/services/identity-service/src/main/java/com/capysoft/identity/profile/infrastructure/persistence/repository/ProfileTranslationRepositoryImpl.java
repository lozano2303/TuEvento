package com.capysoft.identity.profile.infrastructure.persistence.repository;

import com.capysoft.identity.profile.domain.model.ProfileTranslation;
import com.capysoft.identity.profile.domain.repository.ProfileTranslationRepository;
import com.capysoft.identity.profile.infrastructure.persistence.mapper.ProfileTranslationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ProfileTranslationRepositoryImpl implements ProfileTranslationRepository {

    private final ProfileTranslationJpaRepository jpaRepository;
    private final ProfileTranslationMapper mapper;

    @Autowired
    public ProfileTranslationRepositoryImpl(ProfileTranslationJpaRepository jpaRepository, 
                                           ProfileTranslationMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<ProfileTranslation> findByProfileAndLanguage(Long profileId, Integer languageId) {
        return jpaRepository.findByProfileIdAndLanguageId(profileId, languageId)
                .map(mapper::toDomain);
    }

    @Override
    public List<ProfileTranslation> findByLanguage(Integer languageId) {
        return jpaRepository.findByLanguageId(languageId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public ProfileTranslation save(ProfileTranslation profileTranslation) {
        var entity = mapper.toEntity(profileTranslation);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public void delete(ProfileTranslation translation) {
        var entity = mapper.toEntity(translation);
        jpaRepository.delete(entity);
    }

    @Override
    public void deleteByProfile(Long profileId) {
        var entities = jpaRepository.findByProfileId(profileId);
        jpaRepository.deleteAll(entities);
    }
}
