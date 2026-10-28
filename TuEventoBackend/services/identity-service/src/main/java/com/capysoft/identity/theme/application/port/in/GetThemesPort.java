package com.capysoft.identity.theme.application.port.in;

import com.capysoft.identity.theme.application.dto.response.ThemeResponse;

import java.util.List;

public interface GetThemesPort {

    List<ThemeResponse> getAll();
    
    List<ThemeResponse> getAll(String languageCode);
}
