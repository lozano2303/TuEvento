package com.capysoft.tuevento.modules.language.application.port.in;

import com.capysoft.tuevento.modules.language.application.dto.response.LanguageResponse;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de entrada para consultar idiomas.
 */
public interface GetLanguagesUseCase {
    
    List<LanguageResponse> getAllActiveLanguages();
    
    List<LanguageResponse> getAllLanguages();
    
    Optional<LanguageResponse> getLanguageById(Long languageId);
}