package com.capysoft.tuevento.modules.language.application.service;

import com.capysoft.tuevento.modules.language.domain.model.TranslationJob;
import com.capysoft.tuevento.modules.language.domain.repository.TranslationJobRepository;
import com.capysoft.tuevento.shared.application.translation.TranslationRequests;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationJobStatus;
import com.capysoft.tuevento.shared.infrastructure.translation.TranslatableContents;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Modo {@code local}: {@link TranslationRequests} atendido por el núcleo de traducción dentro del
 * monolito.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "translation.mode", havingValue = "local", matchIfMissing = true)
public class LocalTranslationRequests implements TranslationRequests {

    private final TranslationService translationService;
    private final TranslationJobRepository translationJobRepository;
    private final TranslatableContents contents;

    @Override
    public void request(String entityType, long entityId) {
        translationService.requestTranslation(entityType, entityId, SOURCE_LANGUAGE,
                contents.sourceTexts(entityType, entityId));
    }

    @Override
    public Optional<TranslationJobStatus> jobStatus(String entityType, long entityId, int languageId) {
        return translationJobRepository
                .findByEntityAndTargetLanguage(entityType, Math.toIntExact(entityId), languageId)
                .map(TranslationJob::getStatus);
    }

    @Override
    public int discard(String entityType, long entityId) {
        return translationJobRepository.deleteByEntity(entityType, Math.toIntExact(entityId));
    }
}
