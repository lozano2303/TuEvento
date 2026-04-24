package com.capysoft.tuevento.modules.theme.application.mapper;

import com.capysoft.tuevento.modules.theme.application.dto.response.ThemeLogResponse;
import com.capysoft.tuevento.modules.theme.application.dto.response.ThemeResponse;
import com.capysoft.tuevento.modules.theme.domain.model.Theme;
import com.capysoft.tuevento.modules.theme.domain.model.ThemeLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ThemeAppMapper {

    @Mapping(source = "id", target = "id")
    ThemeResponse toThemeResponse(Theme theme);

    @Mapping(source = "id", target = "logId")
    ThemeLogResponse toThemeLogResponse(ThemeLog log);
}
