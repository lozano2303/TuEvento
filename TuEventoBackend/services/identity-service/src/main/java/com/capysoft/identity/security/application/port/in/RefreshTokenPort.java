package com.capysoft.identity.security.application.port.in;

import com.capysoft.identity.security.application.dto.request.RefreshTokenRequest;
import com.capysoft.identity.security.application.dto.response.RefreshTokenResponse;

public interface RefreshTokenPort {

    RefreshTokenResponse refresh(RefreshTokenRequest request);
}
