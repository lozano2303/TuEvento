package com.capysoft.tuevento.modules.profile.interfaces.rest;

import com.capysoft.tuevento.modules.profile.application.dto.request.CreateProfileRequest;
import com.capysoft.tuevento.modules.profile.application.dto.request.UpdateProfileRequest;
import com.capysoft.tuevento.modules.profile.application.dto.response.ProfileResponse;
import com.capysoft.tuevento.modules.profile.application.port.in.CreateProfilePort;
import com.capysoft.tuevento.modules.profile.application.port.in.GetProfileByUserIdPort;
import com.capysoft.tuevento.modules.profile.application.port.in.GetProfilePort;
import com.capysoft.tuevento.modules.profile.application.port.in.UpdateProfilePort;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
@Tag(name = "Profiles", description = "User profile endpoints")
public class ProfileController {

    private final CreateProfilePort       createProfilePort;
    private final UpdateProfilePort       updateProfilePort;
    private final GetProfilePort          getProfilePort;
    private final GetProfileByUserIdPort  getProfileByUserIdPort;

    @Operation(summary = "Create a profile for a user")
    @PostMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> create(
            @Valid @RequestBody CreateProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Profile created successfully",
                        createProfilePort.create(request)));
    }

    @Operation(summary = "Update an existing profile")
    @PutMapping("/{profileId}")
    public ResponseEntity<ApiResponse<ProfileResponse>> update(
            @PathVariable Long profileId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully",
                updateProfilePort.update(profileId, request)));
    }

    @Operation(summary = "Get profile by ID")
    @GetMapping("/{profileId}")
    public ResponseEntity<ApiResponse<ProfileResponse>> getById(
            @PathVariable Long profileId) {
        return ResponseEntity.ok(ApiResponse.ok("Profile retrieved successfully",
                getProfilePort.getById(profileId)));
    }

    @Operation(summary = "Get profile by user ID")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<ProfileResponse>> getByUserId(
            @PathVariable Integer userId) {
        return ResponseEntity.ok(ApiResponse.ok("Profile retrieved successfully",
                getProfileByUserIdPort.getByUserId(userId)));
    }
}
