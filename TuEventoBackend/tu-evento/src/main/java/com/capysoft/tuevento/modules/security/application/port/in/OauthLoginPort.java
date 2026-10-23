package com.capysoft.tuevento.modules.security.application.port.in;

import com.capysoft.tuevento.modules.security.application.dto.OauthProfile;
import com.capysoft.tuevento.modules.security.application.dto.response.LoginResponse;

public interface OauthLoginPort {

    /** Code-based flow: exchanges the authorization code via the provider's token endpoint. */
    LoginResponse login(String provider, String code);

    /**
     * Profile-based flow: the caller has already obtained and verified the user's profile
     * (e.g. from a Google ID Token verified server-side).  Bypasses the code exchange
     * step but reuses the full user-resolution, registration, and token-generation logic.
     */
    LoginResponse loginWithProfile(String provider, OauthProfile profile);
}
