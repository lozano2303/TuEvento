package com.capysoft.tuevento.modules.security.application.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private final String  accessToken;
    private final String  refreshToken;
    private final Integer userId;
    private final String  alias;
    private final String  role;
    /**
     * True when the user registered via OAuth and Google did not provide a name
     * that passes the full-name validation rule.  The frontend should redirect
     * such users to a profile-completion (onboarding) step before sending them
     * to the home page.
     */
    private final boolean needsOnboarding;
    /**
     * Profile ID when a profile row already exists for this user (may be null
     * for brand-new users whose profile creation failed).  The frontend uses
     * this to decide whether to call PUT /profiles/{id} (update) or
     * POST /profiles (create) during the onboarding step.
     */
    private final Long profileId;
}
