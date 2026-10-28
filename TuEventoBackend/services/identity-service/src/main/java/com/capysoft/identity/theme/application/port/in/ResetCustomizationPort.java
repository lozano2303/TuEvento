package com.capysoft.identity.theme.application.port.in;

import com.capysoft.identity.theme.application.dto.response.ResolvedPaletteResponse;

public interface ResetCustomizationPort {

    ResolvedPaletteResponse reset(Integer userId, String property);
}
