package com.capysoft.tuevento.modules.theme.application.port.in;

import com.capysoft.tuevento.modules.theme.application.dto.response.ResolvedPaletteResponse;

public interface ActivateThemePort {

    ResolvedPaletteResponse activate(Integer userId, Integer themeId);
}
