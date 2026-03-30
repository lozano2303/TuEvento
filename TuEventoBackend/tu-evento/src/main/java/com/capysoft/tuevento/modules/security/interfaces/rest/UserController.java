package com.capysoft.tuevento.modules.security.interfaces.rest;

import com.capysoft.tuevento.modules.security.application.dto.request.*;
import com.capysoft.tuevento.modules.security.application.dto.response.*;
import com.capysoft.tuevento.modules.security.application.port.in.*;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Protected endpoints for authenticated users")
public class UserController {

    private final ChangePasswordPort   changePasswordPort;
    private final LinkOauthAccountPort linkOauthAccountPort;
    private final RequestOrganizerPort requestOrganizerPort;

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

    @Operation(summary = "Submit a request to become an organizer")
    @PostMapping(value = "/organizer-request", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<RequestOrganizerResponse>> requestOrganizer(
            @RequestParam("document") MultipartFile document) {
        RequestOrganizerRequest request = RequestOrganizerRequest.builder()
                .document(document)
                .build();
        RequestOrganizerResponse response = requestOrganizerPort.request(request);
        return ResponseEntity.ok(ApiResponse.ok("Organizer request submitted successfully", response));
    }
}
