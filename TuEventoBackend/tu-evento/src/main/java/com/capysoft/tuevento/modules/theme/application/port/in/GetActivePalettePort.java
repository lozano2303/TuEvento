package com.capysoft.tuevento.modules.theme.application.port.in;

import com.capysoft.tuevento.modules.theme.application.dto.response.ResolvedPaletteResponse;

public interface GetActivePalettePort {

    ResolvedPaletteResponse getActivePalette(Integer userId);
}
