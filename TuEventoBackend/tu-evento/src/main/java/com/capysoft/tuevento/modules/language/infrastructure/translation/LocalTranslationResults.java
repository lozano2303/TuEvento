package com.capysoft.tuevento.modules.language.infrastructure.translation;

import com.capysoft.tuevento.modules.language.application.port.out.TranslationResults;
import com.capysoft.tuevento.shared.infrastructure.translation.TranslatableContents;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * {@link TranslationResults} cuando el núcleo corre dentro del monolito: guarda la traducción
 * directamente en el dominio.
 */
@Component
@RequiredArgsConstructor
public class LocalTranslationResults implements TranslationResults {

    private final TranslatableContents contents;

    @Override
    public void deliver(String entityType, long entityId, String languageCode, Map<String, String> translatedTexts) {
        contents.saveMachineTranslation(entityType, entityId, languageCode, translatedTexts);
    }
}
