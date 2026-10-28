package com.capysoft.geolocation.language.application.port.in;

import com.capysoft.geolocation.language.application.dto.response.LanguageResponse;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de entrada para consultar idiomas.
 */
public interface GetLanguagesUseCase {
    
    List<LanguageResponse> getAllActiveLanguages();
    
    List<LanguageResponse> getAllLanguages();
    
    Optional<LanguageResponse> getLanguageById(Integer languageId);
}