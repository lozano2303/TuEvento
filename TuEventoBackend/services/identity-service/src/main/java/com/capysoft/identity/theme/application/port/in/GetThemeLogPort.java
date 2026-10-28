package com.capysoft.identity.theme.application.port.in;

import com.capysoft.identity.theme.application.dto.response.ThemeLogResponse;

import java.util.List;

public interface GetThemeLogPort {

    List<ThemeLogResponse> getLog(Integer userId);
}
