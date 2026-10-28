package com.capysoft.identity.theme.application.mapper;

import com.capysoft.identity.theme.application.dto.response.ThemeLogResponse;
import com.capysoft.identity.theme.application.dto.response.ThemeResponse;
import com.capysoft.identity.theme.domain.model.Theme;
import com.capysoft.identity.theme.domain.model.ThemeLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ThemeAppMapper {

    @Mapping(source = "id", target = "id")
    ThemeResponse toThemeResponse(Theme theme);

    @Mapping(source = "id", target = "logId")
    ThemeLogResponse toThemeLogResponse(ThemeLog log);
}
