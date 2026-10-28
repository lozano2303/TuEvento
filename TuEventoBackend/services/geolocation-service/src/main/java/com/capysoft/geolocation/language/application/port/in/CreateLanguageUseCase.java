package com.capysoft.geolocation.language.application.port.in;

import com.capysoft.geolocation.language.application.dto.request.CreateLanguageRequest;
import com.capysoft.geolocation.language.application.dto.response.LanguageResponse;

/**
 * Puerto de entrada para crear un nuevo idioma.
 */
public interface CreateLanguageUseCase {
    
    LanguageResponse execute(CreateLanguageRequest request);
}