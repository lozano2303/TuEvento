package com.capysoft.tuevento.modules.language.domain.repository;

import com.capysoft.tuevento.modules.language.domain.model.ActionTranslation;

import java.util.List;
import java.util.Optional;

public interface ActionTranslationRepository {

    ActionTranslation save(ActionTranslation translation);

    Optional<ActionTranslation> findById(Integer id);

    List<ActionTranslation> findByAction(String action);

    Optional<ActionTranslation> findByActionAndLanguageId(String action, Integer languageId);

    List<ActionTranslation> findByLanguageId(Integer languageId);

    void delete(ActionTranslation translation);

    void deleteById(Integer id);

    boolean existsById(Integer id);

    List<ActionTranslation> findAll();
}