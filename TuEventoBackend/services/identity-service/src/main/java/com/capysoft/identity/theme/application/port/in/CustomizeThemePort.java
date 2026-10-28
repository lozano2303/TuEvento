package com.capysoft.identity.theme.application.port.in;

import com.capysoft.identity.theme.application.dto.request.CustomizeThemeRequest;
import com.capysoft.identity.theme.application.dto.response.ResolvedPaletteResponse;

public interface CustomizeThemePort {

    ResolvedPaletteResponse customize(Integer userId, CustomizeThemeRequest request);
}
