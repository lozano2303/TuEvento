package com.capysoft.tuevento.modules.language.application.port.in;

import com.capysoft.tuevento.modules.language.application.dto.request.UpdateLanguageRequest;
import com.capysoft.tuevento.modules.language.application.dto.response.LanguageResponse;

/**
 * Puerto de entrada para actualizar idiomas.
 */
public interface UpdateLanguageUseCase {
    
    LanguageResponse updateLanguage(Long languageId, UpdateLanguageRequest request);
    
    LanguageResponse activateLanguage(Long languageId);
    
    LanguageResponse deactivateLanguage(Long languageId);

    /**
     * Establece un idioma como el por defecto.
     */
    LanguageResponse setDefaultLanguage(Long languageId);
}