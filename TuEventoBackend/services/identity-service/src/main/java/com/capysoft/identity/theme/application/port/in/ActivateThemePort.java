package com.capysoft.identity.theme.application.port.in;

import com.capysoft.identity.theme.application.dto.response.ResolvedPaletteResponse;

public interface ActivateThemePort {

    ResolvedPaletteResponse activate(Integer userId, Integer themeId);
}
