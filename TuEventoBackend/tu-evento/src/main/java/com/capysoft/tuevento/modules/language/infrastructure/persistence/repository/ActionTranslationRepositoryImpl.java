package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.domain.model.ActionTranslation;
import com.capysoft.tuevento.modules.language.domain.repository.ActionTranslationRepository;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper.ActionTranslationMapper;
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
    public ActionTranslation save(ActionTranslation translation) {
        var entity = mapper.toEntity(translation);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<ActionTranslation> findById(Integer id) {
        return jpaRepository.findById(id)
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
    public Optional<ActionTranslation> findByActionAndLanguageId(String action, Integer languageId) {
        return jpaRepository.findByActionAndLanguageId(action, languageId)
                .map(mapper::toDomain);
    }

    @Override
    public List<ActionTranslation> findByLanguageId(Integer languageId) {
        return jpaRepository.findByLanguageId(languageId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(ActionTranslation translation) {
        var entity = mapper.toEntity(translation);
        jpaRepository.delete(entity);
    }

    @Override
    public void deleteById(Integer id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Integer id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public List<ActionTranslation> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}