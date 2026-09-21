package com.capysoft.tuevento.modules.security.interfaces.rest;

import com.capysoft.tuevento.modules.security.application.dto.request.*;
import com.capysoft.tuevento.modules.security.application.dto.response.*;
import com.capysoft.tuevento.modules.security.application.port.in.*;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import com.capysoft.tuevento.modules.security.application.dto.request.ConfirmReactivationRequest;
import com.capysoft.tuevento.modules.security.application.dto.request.GoogleAuthRequest;
import com.capysoft.tuevento.modules.security.application.dto.request.ReactivateAccountRequest;
import com.capysoft.tuevento.modules.security.application.port.in.GoogleAuthPort;
import com.capysoft.tuevento.modules.security.domain.repository.LoginCredentialsRepository;
import com.capysoft.tuevento.modules.security.domain.repository.AccountActivationRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Public authentication endpoints")
public class AuthController {

    private final RegisterUserPort    registerUserPort;
    private final ActivateAccountPort activateAccountPort;
    private final LoginPort           loginPort;
    private final LogoutPort          logoutPort;
    private final RefreshTokenPort    refreshTokenPort;
    private final RecoverPasswordPort recoverPasswordPort;
    private final ResetPasswordPort   resetPasswordPort;
    private final OauthLoginPort      oauthLoginPort;
    private final GoogleAuthPort      googleAuthPort;
    private final RequestReactivationPort requestReactivationPort;
    private final ConfirmReactivationPort confirmReactivationPort;
    private final LoginCredentialsRepository loginCredentialsRepository;
    private final AccountActivationRepository accountActivationRepository;

    /** Base OAuth provider authorization URLs (without state) — built by OauthConfig. */
    private final Map<String, String> oauthAuthorizationUrls;

    /** Whitelist of frontend URIs allowed as final redirect destinations after OAuth. */
    private final Set<String> oauthAllowedFrontendRedirectUris;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Operation(summary = "Register a new user account")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterUserResponse>> register(
            @Valid @RequestBody RegisterUserRequest request) {
        RegisterUserResponse response = registerUserPort.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("User registered successfully. Check your email to activate your account.", response));
    }

    @Operation(summary = "Activate account with the code sent by email")
    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<Void>> activate(
            @Valid @RequestBody ActivateAccountRequest request) {
        try {
            System.out.println("=== AUTH CONTROLLER ACTIVATE ===");
            System.out.println("Email: " + request.getEmail());
            System.out.println("Activation Code: " + request.getActivationCode());
            System.out.println("About to call activateAccountPort.activate()");
            activateAccountPort.activate(request);
            System.out.println("activateAccountPort.activate() completed successfully");
            return ResponseEntity.ok(ApiResponse.ok("Account activated successfully"));
        } catch (Exception e) {
            System.out.println("ERROR IN AUTH CONTROLLER: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Operation(summary = "Login with email and password")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        LoginResponse response = loginPort.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @Operation(summary = "Logout and revoke current session")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest request) {
        logoutPort.logout(request);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully"));
    }

    @Operation(summary = "Refresh access token using a valid refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        RefreshTokenResponse response = refreshTokenPort.refresh(request);
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed successfully", response));
    }

    @Operation(summary = "Request a password recovery code by email")
    @PostMapping("/recover-password")
    public ResponseEntity<ApiResponse<Void>> recoverPassword(
            @Valid @RequestBody RecoverPasswordRequest request) {
        recoverPasswordPort.recover(request);
        return ResponseEntity.ok(ApiResponse.ok("Recovery code sent to your email"));
    }

    @Operation(summary = "Reset password using the recovery code")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        resetPasswordPort.reset(request);
        return ResponseEntity.ok(ApiResponse.ok("Password reset successfully"));
    }

    @Operation(summary = "Redirect to OAuth provider authorization page",
               description = """
                   Initiates the Authorization Code flow for the given provider.
                   Optional query params:
                   - state: CSRF token generated by the frontend (passed through to the provider and back)
                   - frontend_redirect_uri: URL the backend will redirect to after a successful callback.
                     Must be on the configured whitelist (app.oauth.allowed-frontend-redirect-uris).
                     Defaults to the first whitelisted URI when absent.
                   """)
    @GetMapping("/oauth/{provider}")
    public ResponseEntity<Void> oauthRedirect(
            @PathVariable String provider,
            @RequestParam(required = false) String state,
            @RequestParam(required = false, name = "frontend_redirect_uri") String frontendRedirectUri) {

        String baseAuthUrl = oauthAuthorizationUrls.get(provider.toLowerCase());
        if (baseAuthUrl == null) {
            return ResponseEntity.badRequest().build();
        }

        // Validate the requested frontend redirect URI against the whitelist.
        // If none is provided, fall back to the first configured allowed URI.
        String resolvedFrontendUri = resolveFrontendRedirectUri(frontendRedirectUri);

        // Encode a composite state value that carries both the original CSRF token
        // (generated by the frontend) and the target frontend redirect URI.
        // The provider returns this value unchanged in the callback, so we can
        // recover both pieces without needing server-side session storage.
        // Shape: {"s":"<original_state>","r":"<frontend_redirect_uri>"}
        String compositeState = buildCompositeState(state, resolvedFrontendUri);
        String encodedState   = URLEncoder.encode(compositeState, StandardCharsets.UTF_8);

        String authUrl = baseAuthUrl + "&state=" + encodedState;

        log.debug("Initiating OAuth redirect — provider={} frontendUri={}", provider, resolvedFrontendUri);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authUrl))
                .build();
    }

    @Operation(summary = "Handle OAuth provider callback — exchanges code for JWT and redirects frontend",
               description = """
                   Receives the authorization code from the OAuth provider, exchanges it for a
                   user profile, creates or links the user account, issues a JWT + refresh token.

                   Web clients (browser-based): respond with a 302 redirect to the frontend URI
                   encoded in the state param carrying token, refreshToken, userID, alias, role,
                   oauth=true, state (original CSRF token), needsOnboarding and profileId.

                   Mobile clients (Expo / React Native): pass response_format=json to receive
                   a 200 JSON response with the same LoginResponse payload instead of a redirect,
                   since deep-link-based flows handle navigation on the client side.

                   Security notes:
                   - The open-redirect whitelist (oauthAllowedFrontendRedirectUris) applies exclusively
                     to the 302 branch, where the token travels as a URL fragment and the destination
                     must be controlled.  In the json branch the token travels in the response body;
                     cross-origin read access from a browser is blocked by CORS
                     (app.cors.allowed-origins whitelist), but CORS does NOT protect against a
                     server-side attacker who intercepts the authorization code and redeems it via
                     curl or their own backend — there is no browser in that path to enforce SOP.
                     The actual mitigation for code interception is:
                       1. HTTPS in production (eliminates the interception window on untrusted networks)
                       2. The authorization code is single-use and expires in minutes (OAuth2 protocol
                          property — no extra code needed)
                     Do not rely on CORS as a substitute for HTTPS in the json branch.
                   """)
    @GetMapping("/oauth/{provider}/callback")
    public ResponseEntity<?> oauthCallback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false, name = "response_format") String responseFormat) {

        LoginResponse response = oauthLoginPort.login(provider, code);

        // ── Mobile JSON response ──────────────────────────────────────────────
        // Native apps (Expo) pass response_format=json.  They are not subject to
        // browser Same-Origin Policy, so CORS does not apply.  The token reaches
        // the app directly and is stored in AsyncStorage, never in a URL.
        if ("json".equalsIgnoreCase(responseFormat)) {
            log.debug("OAuth callback (json) — provider={} userId={}", provider, response.getUserId());
            return ResponseEntity.ok(ApiResponse.ok("OAuth login successful", response));
        }

        // ── Browser 302 redirect ──────────────────────────────────────────────
        // The token travels in the redirect URL, so the destination MUST be on
        // the whitelist to prevent open-redirect token theft.
        // Recover the frontend redirect URI and original CSRF token from the
        // composite state value that the OAuth provider returned unchanged.
        String frontendUri   = oauthAllowedFrontendRedirectUris.iterator().next(); // safe default
        String originalState = null;

        if (state != null && !state.isBlank()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> parsed = objectMapper.readValue(state, Map.class);
                originalState = parsed.get("s");
                String requestedUri = parsed.get("r");
                if (requestedUri != null && oauthAllowedFrontendRedirectUris.contains(requestedUri)) {
                    frontendUri = requestedUri;
                } else if (requestedUri != null) {
                    log.warn("OAuth callback received unlisted frontend_redirect_uri '{}' — using default", requestedUri);
                }
            } catch (Exception e) {
                log.warn("OAuth callback: could not parse composite state '{}' — using defaults", state);
            }
        }

        StringBuilder redirect = new StringBuilder(frontendUri)
                .append("?oauth=true")
                .append("&token=").append(enc(response.getAccessToken()))
                .append("&refreshToken=").append(enc(response.getRefreshToken()))
                .append("&userID=").append(response.getUserId())
                .append("&alias=").append(enc(response.getAlias()))
                .append("&role=").append(enc(response.getRole()))
                .append("&needsOnboarding=").append(response.isNeedsOnboarding());

        if (response.getProfileId() != null) {
            redirect.append("&profileId=").append(response.getProfileId());
        }
        if (originalState != null && !originalState.isBlank()) {
            redirect.append("&state=").append(enc(originalState));
        }

        log.debug("OAuth callback (302) — provider={} userId={} redirect={}", provider, response.getUserId(), frontendUri);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirect.toString()))
                .build();
    }

        @Operation(summary = "Authenticate with a Google ID Token from Google Identity Services (GSI)")
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<LoginResponse>> googleAuth(
            @Valid @RequestBody GoogleAuthRequest request) {
        LoginResponse response = googleAuthPort.authenticate(request);
        return ResponseEntity.ok(ApiResponse.ok("Login con Google exitoso", response));
    }

    @Operation(summary = "Resend activation code to email")
    @PostMapping("/resend-activation")
public ResponseEntity<ApiResponse<Void>> resendActivation(
        @Valid @RequestBody ResendActivationRequest request) {
    registerUserPort.resendActivationCode(request.getEmail());
    return ResponseEntity.ok(ApiResponse.ok("Se ha enviado un nuevo código de activación a tu correo"));
}

@Operation(summary = "Request account reactivation — sends a one-time token to the registered email")
@PostMapping("/reactivate-request")
public ResponseEntity<ApiResponse<Void>> requestReactivation(
        @Valid @RequestBody ReactivateAccountRequest request) {
    requestReactivationPort.request(request);
    // Always 200 regardless of whether the email exists or the account is INACTIVE
    // to prevent user enumeration (same pattern as /recover-password).
    return ResponseEntity.ok(ApiResponse.ok(
            "Si el correo corresponde a una cuenta desactivada, recibirás un código de reactivación en los próximos minutos."));
}

@Operation(summary = "Confirm account reactivation with the one-time token received by email")
@PostMapping("/reactivate-confirm")
public ResponseEntity<ApiResponse<Void>> confirmReactivation(
        @Valid @RequestBody ConfirmReactivationRequest request) {
    confirmReactivationPort.confirm(request);
    return ResponseEntity.ok(ApiResponse.ok("Tu cuenta ha sido reactivada exitosamente. Ya puedes iniciar sesión."));
}

@Operation(summary = "Get activation code for testing (dev only)")
@GetMapping("/dev/activate-code/{email}")
public ResponseEntity<ApiResponse<String>> getActivationCodeForTesting(
        @PathVariable String email) {
    var credentials = loginCredentialsRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException("EMAIL_NOT_FOUND", "Email not found"));
    
    var activation = accountActivationRepository.findByUserAndActivatedFalse(credentials.getUser());
    if (activation.isEmpty()) {
        throw new BusinessException("NO_ACTIVATION_CODE", "No pending activation code");
    }
    
    return ResponseEntity.ok(ApiResponse.ok("Activation code", activation.get().getActivationCode()));
}

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Validates the requested frontend redirect URI against the whitelist.
     * Returns the first whitelisted URI as a safe default when the param is
     * absent or not on the list.
     */
    private String resolveFrontendRedirectUri(String requested) {
        if (requested != null && !requested.isBlank()
                && oauthAllowedFrontendRedirectUris.contains(requested)) {
            return requested;
        }
        if (requested != null && !requested.isBlank()) {
            log.warn("OAuth: requested frontend_redirect_uri '{}' is not whitelisted — using default", requested);
        }
        return oauthAllowedFrontendRedirectUris.iterator().next();
    }

    /**
     * Builds the composite state JSON string that travels through the OAuth provider
     * and back.  Keeps both pieces together without server-side session storage:
     * - "s": the original CSRF token from the frontend (may be null for mobile flows)
     * - "r": the validated frontend redirect URI to send the browser to after callback
     */
    private String buildCompositeState(String originalState, String frontendRedirectUri) {
        try {
            Map<String, String> payload = new java.util.LinkedHashMap<>();
            payload.put("s", originalState != null ? originalState : "");
            payload.put("r", frontendRedirectUri);
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            // Should never happen with simple string values — fall back to a plain string
            return (originalState != null ? originalState : "") + "|" + frontendRedirectUri;
        }
    }

    /** URL-encodes a value for use in a query string. */
    private static String enc(String value) {
        return value == null ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
