package com.capysoft.identity.theme.application.port.in;

import com.capysoft.identity.theme.application.dto.response.ResolvedPaletteResponse;

public interface GetActivePalettePort {

    ResolvedPaletteResponse getActivePalette(Integer userId);
}
