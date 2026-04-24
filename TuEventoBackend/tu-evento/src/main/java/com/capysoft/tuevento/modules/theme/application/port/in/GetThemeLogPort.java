package com.capysoft.tuevento.modules.theme.application.port.in;

import com.capysoft.tuevento.modules.theme.application.dto.response.ThemeLogResponse;

import java.util.List;

public interface GetThemeLogPort {

    List<ThemeLogResponse> getLog(Integer userId);
}
