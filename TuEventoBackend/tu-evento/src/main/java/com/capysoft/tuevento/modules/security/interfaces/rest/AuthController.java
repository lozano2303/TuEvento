package com.capysoft.tuevento.modules.security.interfaces.rest;

import com.capysoft.tuevento.modules.security.application.dto.request.*;
import com.capysoft.tuevento.modules.security.application.dto.response.*;
import com.capysoft.tuevento.modules.security.application.port.in.*;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

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

    /** OAuth provider authorization URLs — extensible by adding entries. */
    private final Map<String, String> oauthAuthorizationUrls;

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
        activateAccountPort.activate(request);
        return ResponseEntity.ok(ApiResponse.ok("Account activated successfully"));
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

    @Operation(summary = "Redirect to OAuth provider authorization page")
    @GetMapping("/oauth/{provider}")
    public ResponseEntity<Void> oauthRedirect(@PathVariable String provider) {
        String authUrl = oauthAuthorizationUrls.get(provider.toLowerCase());
        if (authUrl == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authUrl))
                .build();
    }

    @Operation(summary = "Handle OAuth provider callback and authenticate user")
    @GetMapping("/oauth/{provider}/callback")
    public ResponseEntity<ApiResponse<LoginResponse>> oauthCallback(
            @PathVariable String provider,
            @RequestParam String code) {
        LoginResponse response = oauthLoginPort.login(provider, code);
        return ResponseEntity.ok(ApiResponse.ok("OAuth login successful", response));
    }
}
