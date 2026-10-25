package com.capysoft.tuevento.shared.infrastructure.config;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.capysoft.tuevento.modules.security.application.dto.OauthProfile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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

    // ── Frontend redirect whitelist ──────────────────────────────────────────

    /**
     * Comma-separated list of allowed frontend callback URLs.
     * The backend validates the frontend_redirect_uri query param against this
     * set before issuing the final 302 to the browser.  Any URL not on this
     * list is rejected to prevent open-redirect attacks.
     *
     * Populated from app.oauth.allowed-frontend-redirect-uris in application-*.yaml.
     */
    @Value("${app.oauth.allowed-frontend-redirect-uris}")
    private List<String> allowedFrontendRedirectUris;

    // ── Beans ────────────────────────────────────────────────────────────────

    /**
     * Registry of provider → profile resolver functions.
     * To add a new provider (e.g. GitHub), register a new entry here.
     */
    @Bean
    public Map<String, Function<String, OauthProfile>> oauthProviderResolvers() {
        return Map.of(
                "google", googleProfileResolver()
        );
    }

    /**
     * Whitelist of frontend redirect URIs allowed as the final destination
     * after a successful OAuth callback.  AuthController validates the
     * frontend_redirect_uri param against this set.
     *
     * Exposed as a bean so AuthController can inject it without coupling to OauthConfig.
     */
    @Bean
    public Set<String> oauthAllowedFrontendRedirectUris() {
        return Set.copyOf(allowedFrontendRedirectUris);
    }

    /**
     * Base authorization URLs per provider — without state or frontend_redirect_uri.
     * AuthController appends those params dynamically at request time so they can
     * vary per call (different browsers/devices may send different redirect targets).
     */
    @Bean
    public Map<String, String> oauthAuthorizationUrls() {
        // NOTE: state is NOT embedded here anymore — AuthController appends it
        // dynamically when the user hits GET /oauth/{provider}.
        String googleUrl = googleAuthorizationUri
                + "?client_id=" + googleClientId
                + "&redirect_uri=" + googleRedirectUri
                + "&response_type=code"
                + "&scope=openid%20email%20profile"
                + "&access_type=offline";

        return Map.of(
                "google", googleUrl
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

            String googleName = (String) userInfo.get("name");
            return OauthProfile.builder()
                    .providerUserId((String) userInfo.get("sub"))
                    .email((String) userInfo.get("email"))
                    .fullName(googleName)   // needed by OauthLoginUseCase to skip onboarding
                    .alias(googleName)
                    .build();
        };
    }
}
