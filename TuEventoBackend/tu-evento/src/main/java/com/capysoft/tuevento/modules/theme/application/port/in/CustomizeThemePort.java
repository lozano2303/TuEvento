package com.capysoft.tuevento.modules.theme.application.port.in;

import com.capysoft.tuevento.modules.theme.application.dto.request.CustomizeThemeRequest;
import com.capysoft.tuevento.modules.theme.application.dto.response.ResolvedPaletteResponse;

public interface CustomizeThemePort {

    ResolvedPaletteResponse customize(Integer userId, CustomizeThemeRequest request);
}
