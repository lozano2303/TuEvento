package com.capysoft.tuevento.modules.language.application.service;

import com.capysoft.tuevento.modules.language.domain.model.Language;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.shared.application.translation.LanguageCatalog;
import com.capysoft.tuevento.shared.application.translation.LanguageSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * {@link LanguageCatalog} atendido por el catálogo de idiomas dentro del monolito.
 */
@Component
@RequiredArgsConstructor
public class LocalLanguageCatalog implements LanguageCatalog {

    private final LanguageRepository languageRepository;

    @Override
    public Optional<LanguageSummary> findByCode(String code) {
        return languageRepository.findByCode(code).map(LocalLanguageCatalog::summary);
    }

    @Override
    public Optional<LanguageSummary> findById(Integer languageId) {
        return languageRepository.findById(languageId).map(LocalLanguageCatalog::summary);
    }

    private static LanguageSummary summary(Language language) {
        return LanguageSummary.builder().languageId(language.getLanguageId()).code(language.getCode()).build();
    }
}
