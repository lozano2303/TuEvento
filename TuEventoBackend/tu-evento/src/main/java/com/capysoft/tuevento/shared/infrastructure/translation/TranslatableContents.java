package com.capysoft.tuevento.shared.infrastructure.translation;

import com.capysoft.tuevento.shared.application.translation.TranslatableContentHandler;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * El contenido traducible de todos los dominios, por tipo de entidad. Del lado de los dominios
 * es lo único que el núcleo de traducción necesita: el texto original para pedir la traducción y
 * dónde dejar el resultado.
 */
@Slf4j
@Component
public class TranslatableContents {

    private final Map<String, TranslatableContentHandler> handlers;

    public TranslatableContents(List<TranslatableContentHandler> handlers) {
        this.handlers = handlers.stream()
                .collect(Collectors.toMap(TranslatableContentHandler::entityType, Function.identity()));
    }

    /** Textos a traducir de la entidad (campo → texto); vacío si no hay nada o el tipo no es traducible. */
    public Map<String, String> sourceTexts(String entityType, long entityId) {
        TranslatableContentHandler handler = handlers.get(entityType);
        if (handler == null) {
            log.warn("No translatable content for entity type {}", entityType);
            return Map.of();
        }
        return handler.loadSourceTexts(entityId);
    }

    /**
     * Guarda una traducción automática, publicada. Una traducción ya publicada (revisada a mano o
     * de una traducción anterior del mismo texto) no se sobrescribe.
     *
     * @return {@code true} si se guardó
     */
    public boolean saveMachineTranslation(String entityType, long entityId, String languageCode,
                                          Map<String, String> translatedTexts) {
        TranslatableContentHandler handler = handlers.get(entityType);
        if (handler == null) {
            log.warn("No translatable content for entity type {}", entityType);
            return false;
        }
        boolean published = handler.findTranslationStatus(entityId, languageCode)
                .filter(status -> status == TranslationStatus.PUBLISHED)
                .isPresent();
        if (published) {
            log.info("Keeping published translation of {}/{} in {}", entityType, entityId, languageCode);
            return false;
        }
        handler.saveTranslation(entityId, languageCode, translatedTexts, TranslationSource.MACHINE, TranslationStatus.PUBLISHED);
        return true;
    }
}
