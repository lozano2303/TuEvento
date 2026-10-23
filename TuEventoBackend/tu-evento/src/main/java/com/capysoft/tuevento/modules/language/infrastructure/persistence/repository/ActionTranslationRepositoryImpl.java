package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.domain.model.ActionTranslation;
import com.capysoft.tuevento.modules.language.domain.repository.ActionTranslationRepository;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper.ActionTranslationMapper;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ActionTranslationRepositoryImpl implements ActionTranslationRepository {

    private final ActionTranslationJpaRepository jpaRepository;
    private final ActionTranslationMapper mapper;

    @Override
    public Optional<ActionTranslation> findByActionAndLanguage(String action, Integer languageId) {
        return jpaRepository.findByActionAndLanguageId(action, languageId)
                .map(mapper::toDomain);
    }

    @Override
    public List<ActionTranslation> findByAction(String action) {
        return jpaRepository.findByAction(action)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ActionTranslation> findByLanguage(Integer languageId) {
        return jpaRepository.findByLanguageId(languageId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ActionTranslation> findByStatus(TranslationStatus status) {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .filter(translation -> translation.getStatus().equals(status))
                .collect(Collectors.toList());
    }

    @Override
    public List<ActionTranslation> findByActionAndStatus(String action, TranslationStatus status) {
        return jpaRepository.findByAction(action)
                .stream()
                .map(mapper::toDomain)
                .filter(translation -> translation.getStatus().equals(status))
                .collect(Collectors.toList());
    }

    @Override
    public ActionTranslation save(ActionTranslation translation) {
        var entity = mapper.toEntity(translation);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public List<ActionTranslation> saveAll(List<ActionTranslation> translations) {
        var entities = translations.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());
        var savedEntities = jpaRepository.saveAll(entities);
        return savedEntities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(ActionTranslation translation) {
        var entity = mapper.toEntity(translation);
        jpaRepository.delete(entity);
    }

    @Override
    public void deleteByAction(String action) {
        var entities = jpaRepository.findByAction(action);
        jpaRepository.deleteAll(entities);
    }

    @Override
    public boolean existsByActionAndLanguage(String action, Integer languageId) {
        return jpaRepository.findByActionAndLanguageId(action, languageId)
                .isPresent();
    }
}