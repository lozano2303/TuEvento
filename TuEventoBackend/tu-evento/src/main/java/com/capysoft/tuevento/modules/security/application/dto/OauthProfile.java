package com.capysoft.tuevento.modules.security.application.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OauthProfile {

    private String providerUserId;
    private String email;
    /** Google / provider display name (e.g. "John Doe"). Used to populate Profile.fullName on first login. */
    private String fullName;
    /** Alias hint from the provider — used as fallback base for alias generation when email is absent. */
    private String alias;
}
