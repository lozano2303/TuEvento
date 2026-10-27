package com.capysoft.tuevento.modules.identitybridge.translation;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.identitybridge.client.IdentityServiceClient;
import com.capysoft.tuevento.shared.application.translation.TranslatableContentHandler;
import com.capysoft.tuevento.shared.domain.exception.ServiceUnavailableException;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handler del motor de traducciones para la biografía de los perfiles. Los perfiles son de
 * identity-service: el texto de origen se lee y la traducción se guarda por su API interna.
 * <ul>
 *   <li>Si no se puede leer el texto, no hay nada que traducir (el motor no crea trabajos).</li>
 *   <li>Si no se puede guardar, la excepción llega al motor, que reintenta el trabajo.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileTranslatableContentHandler implements TranslatableContentHandler {

    /** Tipo de entidad con el que se registran los trabajos de traducción de perfiles. */
    public static final String ENTITY_TYPE = "profile";

    private final IdentityServiceClient identityServiceClient;

    @Override
    public String entityType() {
        return ENTITY_TYPE;
    }

    @Override
    public Map<String, String> loadSourceTexts(Long profileId) {
        try {
            return identityServiceClient.profileTranslatableTexts(profileId);
        } catch (ServiceUnavailableException e) {
            log.warn("Profile {} source texts unavailable, nothing to translate now: {}", profileId, e.getMessage());
            return Map.of();
        }
    }

    @Override
    public void saveTranslation(Long entityId, String languageCode, Map<String, String> translatedTexts,
                                TranslationSource source, TranslationStatus status) {
        identityServiceClient.saveProfileTranslation(entityId, languageCode, translatedTexts, source, status);
    }

    @Override
    public Optional<TranslationStatus> findTranslationStatus(Long entityId, String languageCode) {
        try {
            return identityServiceClient.profileTranslationStatus(entityId, languageCode);
        } catch (ServiceUnavailableException e) {
            return Optional.empty();
        }
    }
}
