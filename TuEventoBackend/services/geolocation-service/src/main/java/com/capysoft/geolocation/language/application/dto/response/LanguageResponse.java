package com.capysoft.geolocation.language.application.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO de respuesta para Language.
 */
@Getter
@Builder
public class LanguageResponse {
    
    private final Integer languageId;
    private final String code;
    private final String name;
    private final Boolean isActive;
    private final Boolean isDefault;
}