package com.capysoft.identity.profile.application.port.out;

import java.util.Optional;

/**
 * Idiomas para las traducciones del perfil. Los aporta Localización; profile solo guarda el
 * {@code languageId}.
 */
public interface LanguageDirectoryPort {

    Optional<Integer> findIdByCode(String languageCode);

    Optional<String> findCodeById(Integer languageId);
}
