package com.capysoft.tuevento.modules.theme.interfaces.rest;

import com.capysoft.tuevento.modules.theme.application.dto.request.CustomizeThemeRequest;
import com.capysoft.tuevento.modules.theme.application.dto.response.ResolvedPaletteResponse;
import com.capysoft.tuevento.modules.theme.application.dto.response.ThemeLogResponse;
import com.capysoft.tuevento.modules.theme.application.dto.response.ThemeResponse;
import com.capysoft.tuevento.modules.theme.application.port.in.ActivateThemePort;
import com.capysoft.tuevento.modules.theme.application.port.in.CustomizeThemePort;
import com.capysoft.tuevento.modules.theme.application.port.in.GetActivePalettePort;
import com.capysoft.tuevento.modules.theme.application.port.in.GetThemeLogPort;
import com.capysoft.tuevento.modules.theme.application.port.in.GetThemesPort;
import com.capysoft.tuevento.modules.theme.application.port.in.ResetCustomizationPort;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityUser;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/themes")
@RequiredArgsConstructor
@Tag(name = "Themes", description = "Theme management and palette customization endpoints")
public class ThemeController {

    private final GetThemesPort        getThemesPort;
    private final ActivateThemePort    activateThemePort;
    private final GetActivePalettePort getActivePalettePort;
    private final CustomizeThemePort   customizeThemePort;
    private final ResetCustomizationPort resetCustomizationPort;
    private final GetThemeLogPort      getThemeLogPort;

    @Operation(summary = "List all available themes — public")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ThemeResponse>>> getThemes() {
        return ResponseEntity.ok(ApiResponse.ok("Themes retrieved successfully",
                getThemesPort.getAll()));
    }

    @Operation(summary = "Activate a theme for the authenticated user")
    @PostMapping("/activate/{themeId}")
    public ResponseEntity<ApiResponse<ResolvedPaletteResponse>> activateTheme(
            @PathVariable Integer themeId,
            @AuthenticationPrincipal SecurityUser securityUser) {
        return ResponseEntity.ok(ApiResponse.ok("Theme activated successfully",
                activateThemePort.activate(securityUser.getUserId(), themeId)));
    }

    @Operation(summary = "Get the active resolved palette for the authenticated user")
    @GetMapping("/my-active")
    public ResponseEntity<ApiResponse<ResolvedPaletteResponse>> getActivePalette(
            @AuthenticationPrincipal SecurityUser securityUser) {
        return ResponseEntity.ok(ApiResponse.ok("Active palette retrieved successfully",
                getActivePalettePort.getActivePalette(securityUser.getUserId())));
    }

    @Operation(summary = "Override a palette property for the authenticated user's active theme")
    @PutMapping("/my-active/customize")
    public ResponseEntity<ApiResponse<ResolvedPaletteResponse>> customize(
            @Valid @RequestBody CustomizeThemeRequest request,
            @AuthenticationPrincipal SecurityUser securityUser) {
        return ResponseEntity.ok(ApiResponse.ok("Theme customized successfully",
                customizeThemePort.customize(securityUser.getUserId(), request)));
    }

    @Operation(summary = "Reset a palette property to its theme default for the authenticated user")
    @DeleteMapping("/my-active/customize/{property}")
    public ResponseEntity<ApiResponse<ResolvedPaletteResponse>> resetCustomization(
            @PathVariable String property,
            @AuthenticationPrincipal SecurityUser securityUser) {
        return ResponseEntity.ok(ApiResponse.ok("Customization reset successfully",
                resetCustomizationPort.reset(securityUser.getUserId(), property)));
    }

    @Operation(summary = "Get the theme change log for the authenticated user's active theme")
    @GetMapping("/my-active/log")
    public ResponseEntity<ApiResponse<List<ThemeLogResponse>>> getLog(
            @AuthenticationPrincipal SecurityUser securityUser) {
        return ResponseEntity.ok(ApiResponse.ok("Theme log retrieved successfully",
                getThemeLogPort.getLog(securityUser.getUserId())));
    }
}
