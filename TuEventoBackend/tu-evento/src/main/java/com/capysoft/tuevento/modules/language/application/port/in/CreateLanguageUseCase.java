package com.capysoft.tuevento.modules.language.application.port.in;

import com.capysoft.tuevento.modules.language.application.dto.request.CreateLanguageRequest;
import com.capysoft.tuevento.modules.language.application.dto.response.LanguageResponse;

/**
 * Puerto de entrada para crear un nuevo idioma.
 */
public interface CreateLanguageUseCase {
    
    LanguageResponse execute(CreateLanguageRequest request);
}