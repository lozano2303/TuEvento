package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.domain.model.ModuleTranslation;
import com.capysoft.tuevento.modules.language.domain.repository.ModuleTranslationRepository;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper.ModuleTranslationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ModuleTranslationRepositoryImpl implements ModuleTranslationRepository {

    private final ModuleTranslationJpaRepository jpaRepository;
    private final ModuleTranslationMapper mapper;

    @Override
    public ModuleTranslation save(ModuleTranslation translation) {
        var entity = mapper.toEntity(translation);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<ModuleTranslation> findById(Integer id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<ModuleTranslation> findByModule(String module) {
        return jpaRepository.findByModule(module)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ModuleTranslation> findByModuleAndLanguageId(String module, Integer languageId) {
        return jpaRepository.findByModuleAndLanguageId(module, languageId)
                .map(mapper::toDomain);
    }

    @Override
    public List<ModuleTranslation> findByLanguageId(Integer languageId) {
        return jpaRepository.findByLanguageId(languageId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(ModuleTranslation translation) {
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
    public List<ModuleTranslation> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}