package com.capysoft.tuevento.modules.security.application.port.in;

import com.capysoft.tuevento.modules.security.application.dto.request.RefreshTokenRequest;
import com.capysoft.tuevento.modules.security.application.dto.response.RefreshTokenResponse;

public interface RefreshTokenPort {

    RefreshTokenResponse refresh(RefreshTokenRequest request);
}
