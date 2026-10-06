package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.domain.model.ModuleTranslation;
import com.capysoft.tuevento.modules.language.domain.repository.ModuleTranslationRepository;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper.ModuleTranslationMapper;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
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
    public Optional<ModuleTranslation> findByModuleAndLanguage(String module, Long languageId) {
        return jpaRepository.findByModuleAndLanguageId(module, languageId.intValue())
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
    public List<ModuleTranslation> findByLanguage(Long languageId) {
        return jpaRepository.findByLanguageId(languageId.intValue())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ModuleTranslation> findByStatus(TranslationStatus status) {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .filter(translation -> translation.getStatus().equals(status))
                .collect(Collectors.toList());
    }

    @Override
    public List<ModuleTranslation> findByModuleAndStatus(String module, TranslationStatus status) {
        return jpaRepository.findByModule(module)
                .stream()
                .map(mapper::toDomain)
                .filter(translation -> translation.getStatus().equals(status))
                .collect(Collectors.toList());
    }

    @Override
    public ModuleTranslation save(ModuleTranslation translation) {
        var entity = mapper.toEntity(translation);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public List<ModuleTranslation> saveAll(List<ModuleTranslation> translations) {
        var entities = translations.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());
        var savedEntities = jpaRepository.saveAll(entities);
        return savedEntities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(ModuleTranslation translation) {
        var entity = mapper.toEntity(translation);
        jpaRepository.delete(entity);
    }

    @Override
    public void deleteByModule(String module) {
        var entities = jpaRepository.findByModule(module);
        jpaRepository.deleteAll(entities);
    }

    @Override
    public boolean existsByModuleAndLanguage(String module, Long languageId) {
        return jpaRepository.findByModuleAndLanguageId(module, languageId.intValue())
                .isPresent();
    }
}