package com.capysoft.tuevento.modules.language.application.mapper;

import com.capysoft.tuevento.modules.language.application.dto.response.LanguageResponse;
import com.capysoft.tuevento.modules.language.domain.model.Language;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Mapper entre dominio y aplicación para Language.
 */
@Mapper(componentModel = "spring")
public interface LanguageAppMapper {
    
    LanguageResponse toResponse(Language language);
    
    List<LanguageResponse> toResponseList(List<Language> languages);
}