package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.domain.model.ProfileTranslation;
import com.capysoft.tuevento.modules.language.domain.repository.ProfileTranslationRepository;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper.ProfileTranslationMapper;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
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
    public List<ProfileTranslation> findByProfile(Long profileId) {
        return jpaRepository.findByProfileId(profileId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProfileTranslation> findByLanguage(Integer languageId) {
        return jpaRepository.findByLanguageId(languageId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProfileTranslation> findByStatus(TranslationStatus status) {
        return jpaRepository.findByStatus(status.name().toLowerCase())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProfileTranslation> findByProfileAndStatus(Long profileId, TranslationStatus status) {
        // This method would need a custom query in JpaRepository, for now filtering in memory
        return findByProfile(profileId)
                .stream()
                .filter(t -> t.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public ProfileTranslation save(ProfileTranslation profileTranslation) {
        var entity = mapper.toEntity(profileTranslation);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public List<ProfileTranslation> saveAll(List<ProfileTranslation> translations) {
        var entities = translations.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());
        var savedEntities = jpaRepository.saveAll(entities);
        return savedEntities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
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

    @Override
    public boolean existsByProfileAndLanguage(Long profileId, Integer languageId) {
        return jpaRepository.findByProfileIdAndLanguageId(profileId, languageId).isPresent();
    }
}