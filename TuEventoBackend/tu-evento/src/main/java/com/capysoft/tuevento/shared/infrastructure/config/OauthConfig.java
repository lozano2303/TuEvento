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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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
                "google",   googleProfileResolver(),
                "facebook", facebookProfileResolver()
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

        String facebookUrl = facebookAuthUri
                + "?client_id=" + facebookClientId
                + "&redirect_uri=" + facebookRedirectUri
                + "&scope=email,public_profile"
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

            String googleName = (String) userInfo.get("name");
            return OauthProfile.builder()
                    .providerUserId((String) userInfo.get("sub"))
                    .email((String) userInfo.get("email"))
                    .fullName(googleName)   // needed by OauthLoginUseCase to skip onboarding
                    .alias(googleName)
                    .build();
        };
    }

    // ── Facebook resolver ────────────────────────────────────────────────────

    private Function<String, OauthProfile> facebookProfileResolver() {
        RestClient restClient = RestClient.create();
        ObjectMapper objectMapper = new ObjectMapper();

        return authorizationCode -> {
            try {
                // Step 1 — exchange authorization code for access token
                // Facebook responds with Content-Type: text/javascript, so we read as String
                // and parse manually to avoid HttpMessageConverter restrictions.
                MultiValueMap<String, String> tokenParams = new LinkedMultiValueMap<>();
                tokenParams.add("code",          authorizationCode);
                tokenParams.add("client_id",     facebookClientId);
                tokenParams.add("client_secret", facebookClientSecret);
                tokenParams.add("redirect_uri",  facebookRedirectUri);

                String tokenRaw = restClient.post()
                        .uri(facebookTokenUri)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(tokenParams)
                        .retrieve()
                        .body(String.class);

                Map<String, Object> tokenResponse = objectMapper.readValue(
                        tokenRaw, new TypeReference<>() {});

                String accessToken = (String) tokenResponse.get("access_token");

                // Step 2 — fetch user profile from Graph API (id differs from Google's sub)
                // Same pattern: read as String, parse with ObjectMapper.
                String profileRaw = restClient.get()
                        .uri(facebookProfileUri + "?fields=id,name,email&access_token=" + accessToken)
                        .retrieve()
                        .body(String.class);

                Map<String, Object> userInfo = objectMapper.readValue(
                        profileRaw, new TypeReference<>() {});

                String facebookName = (String) userInfo.get("name");
                return OauthProfile.builder()
                        .providerUserId((String) userInfo.get("id"))
                        .email((String) userInfo.get("email"))
                        .fullName(facebookName)   // needed by OauthLoginUseCase to skip onboarding
                        .alias(facebookName)
                        .build();

            } catch (Exception e) {
                throw new com.capysoft.tuevento.shared.domain.exception.BusinessException(
                        "FACEBOOK_OAUTH_FAILED", "Facebook OAuth failed: " + e.getMessage());
            }
        };
    }
}
