package com.capysoft.identity.security.interfaces.rest;

import com.capysoft.identity.security.application.dto.request.*;
import com.capysoft.identity.security.application.dto.response.*;
import com.capysoft.identity.security.application.port.in.*;
import com.capysoft.common.interfaces.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Protected endpoints for authenticated users")
// La solicitud de organizador (POST /organizer-request, GET /organizer-petition) sigue en el monolito
// hasta el corte del panel de admin: sube el documento a Archivos y la revisan los admins
public class UserController {

    private final ChangePasswordPort    changePasswordPort;
    private final LinkOauthAccountPort  linkOauthAccountPort;
    private final DeactivateAccountPort deactivateAccountPort;

    @Operation(summary = "Change password for the authenticated user")
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        changePasswordPort.change(request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully"));
    }

    @Operation(summary = "Link an OAuth provider account to the authenticated user")
    @PostMapping("/oauth/link")
    public ResponseEntity<ApiResponse<LinkOauthAccountResponse>> linkOauth(
            @Valid @RequestBody LinkOauthAccountRequest request) {
        LinkOauthAccountResponse response = linkOauthAccountPort.link(request);
        return ResponseEntity.ok(ApiResponse.ok("OAuth account linked successfully", response));
    }

    @Operation(summary = "Deactivate the authenticated user's account")
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(
            @Valid @RequestBody DeactivateAccountRequest request) {
        deactivateAccountPort.deactivate(request);
        return ResponseEntity.ok(ApiResponse.ok("Account deactivated successfully"));
    }
}
