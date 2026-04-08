package com.capysoft.tuevento.shared.infrastructure.config;

import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.capysoft.tuevento.modules.security.application.dto.OauthProfile;

@Configuration
public class OauthConfig {

    // ── Google properties ────────────────────────────────────────────────────

    @Value("${app.oauth.google.client-id}")
    private String googleClientId;

    @Value("${app.oauth.google.client-secret}")
    private String googleClientSecret;

    @Value("${app.oauth.google.redirect-uri}")
    private String googleRedirectUri;

    @Value("${app.oauth.google.token-uri}")
    private String googleTokenUri;

    @Value("${app.oauth.google.userinfo-uri}")
    private String googleUserinfoUri;

    @Value("${app.oauth.google.authorization-uri}")
    private String googleAuthorizationUri;

    // ── Facebook properties ──────────────────────────────────────────────────

    @Value("${app.oauth.facebook.client-id}")
    private String facebookClientId;

    @Value("${app.oauth.facebook.client-secret}")
    private String facebookClientSecret;

    @Value("${app.oauth.facebook.redirect-uri}")
    private String facebookRedirectUri;

    @Value("${app.oauth.facebook.auth-uri}")
    private String facebookAuthUri;

    @Value("${app.oauth.facebook.token-uri}")
    private String facebookTokenUri;

    @Value("${app.oauth.facebook.profile-uri}")
    private String facebookProfileUri;

    // ── Beans ────────────────────────────────────────────────────────────────

    /**
     * Registry of provider → profile resolver functions.
     * To add a new provider (e.g. GitHub), register a new entry here.
     */
    @Bean
    public Map<String, Function<String, OauthProfile>> oauthProviderResolvers() {
        return Map.of(
                "google",   googleProfileResolver(),
                "facebook", facebookProfileResolver()
        );
    }

    /**
     * Authorization URLs per provider, used by AuthController to redirect the user.
     */
    @Bean
    public Map<String, String> oauthAuthorizationUrls() {
        String googleUrl = googleAuthorizationUri
                + "?client_id=" + googleClientId
                + "&redirect_uri=" + googleRedirectUri
                + "&response_type=code"
                + "&scope=openid%20email%20profile"
                + "&access_type=offline";

        String facebookUrl = facebookAuthUri
                + "?client_id=" + facebookClientId
                + "&redirect_uri=" + facebookRedirectUri
                + "&scope=public_profile"
                + "&response_type=code";

        return Map.of(
                "google",   googleUrl,
                "facebook", facebookUrl
        );
    }

    // ── Google resolver ──────────────────────────────────────────────────────

    private Function<String, OauthProfile> googleProfileResolver() {
        RestClient restClient = RestClient.create();

        return authorizationCode -> {
            // Step 1 — exchange authorization code for access token
            MultiValueMap<String, String> tokenParams = new LinkedMultiValueMap<>();
            tokenParams.add("code",          authorizationCode);
            tokenParams.add("client_id",     googleClientId);
            tokenParams.add("client_secret", googleClientSecret);
            tokenParams.add("redirect_uri",  googleRedirectUri);
            tokenParams.add("grant_type",    "authorization_code");

            @SuppressWarnings("unchecked")
            Map<String, Object> tokenResponse = restClient.post()
                    .uri(googleTokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(tokenParams)
                    .retrieve()
                    .body(Map.class);

            String accessToken = (String) tokenResponse.get("access_token");

            // Step 2 — fetch user profile from Google userinfo endpoint
            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = restClient.get()
                    .uri(googleUserinfoUri)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            return OauthProfile.builder()
                    .providerUserId((String) userInfo.get("sub"))
                    .email((String) userInfo.get("email"))
                    .alias((String) userInfo.get("name"))
                    .build();
        };
    }

    // ── Facebook resolver ────────────────────────────────────────────────────

    private Function<String, OauthProfile> facebookProfileResolver() {
        RestClient restClient = RestClient.create();

        return authorizationCode -> {
            // Step 1 — exchange authorization code for access token
            MultiValueMap<String, String> tokenParams = new LinkedMultiValueMap<>();
            tokenParams.add("code",          authorizationCode);
            tokenParams.add("client_id",     facebookClientId);
            tokenParams.add("client_secret", facebookClientSecret);
            tokenParams.add("redirect_uri",  facebookRedirectUri);

            @SuppressWarnings("unchecked")
            Map<String, Object> tokenResponse = restClient.post()
                    .uri(facebookTokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(tokenParams)
                    .retrieve()
                    .body(Map.class);

            String accessToken = (String) tokenResponse.get("access_token");

            // Step 2 — fetch user profile from Graph API (id differs from Google's sub)
            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = restClient.get()
                    .uri(facebookProfileUri + "?fields=id,name,email&access_token=" + accessToken)
                    .retrieve()
                    .body(Map.class);

            return OauthProfile.builder()
                    .providerUserId((String) userInfo.get("id"))
                    .email((String) userInfo.get("email"))
                    .alias((String) userInfo.get("name"))
                    .build();
        };
    }
}
