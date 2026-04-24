package com.capysoft.tuevento.modules.theme.application.port.in;

import com.capysoft.tuevento.modules.theme.application.dto.response.ThemeResponse;

import java.util.List;

public interface GetThemesPort {

    List<ThemeResponse> getAll();
}
