package com.capysoft.tuevento.modules.language.domain.repository;

import com.capysoft.tuevento.modules.language.domain.model.ModuleTranslation;

import java.util.List;
import java.util.Optional;

public interface ModuleTranslationRepository {

    ModuleTranslation save(ModuleTranslation translation);

    Optional<ModuleTranslation> findById(Integer id);

    List<ModuleTranslation> findByModule(String module);

    Optional<ModuleTranslation> findByModuleAndLanguageId(String module, Integer languageId);

    List<ModuleTranslation> findByLanguageId(Integer languageId);

    void delete(ModuleTranslation translation);

    void deleteById(Integer id);

    boolean existsById(Integer id);

    List<ModuleTranslation> findAll();
}