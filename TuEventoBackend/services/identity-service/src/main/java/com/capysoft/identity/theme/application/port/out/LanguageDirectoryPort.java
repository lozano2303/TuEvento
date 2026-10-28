package com.capysoft.identity.theme.application.port.out;

import java.util.Optional;

/** Idiomas para las traducciones de los temas. Los aporta Localización. */
public interface LanguageDirectoryPort {

    Optional<Integer> findIdByCode(String languageCode);
}
