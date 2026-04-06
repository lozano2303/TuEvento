package com.capysoft.tuevento.modules.geolocation.interfaces.rest;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.capysoft.tuevento.modules.geolocation.application.dto.request.CreateSiteRequest;
import com.capysoft.tuevento.modules.geolocation.application.dto.request.UpdateSiteRequest;
import com.capysoft.tuevento.modules.geolocation.application.dto.response.CityResponse;
import com.capysoft.tuevento.modules.geolocation.application.dto.response.DepartmentResponse;
import com.capysoft.tuevento.modules.geolocation.application.dto.response.SiteResponse;
import com.capysoft.tuevento.modules.geolocation.application.port.in.CreateSitePort;
import com.capysoft.tuevento.modules.geolocation.application.port.in.GetCitiesByDepartmentPort;
import com.capysoft.tuevento.modules.geolocation.application.port.in.GetDepartmentsPort;
import com.capysoft.tuevento.modules.geolocation.application.port.in.GetNearbySitesPort;
import com.capysoft.tuevento.modules.geolocation.application.port.in.GetSitePort;
import com.capysoft.tuevento.modules.geolocation.application.port.in.GetSitesByCityPort;
import com.capysoft.tuevento.modules.geolocation.application.port.in.GetSitesByDepartmentPort;
import com.capysoft.tuevento.modules.geolocation.application.port.in.UpdateSitePort;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/geolocation")
@RequiredArgsConstructor
@Tag(name = "Geolocation", description = "Departments, cities and sites endpoints")
public class GeolocationController {

    private final GetDepartmentsPort      getDepartmentsPort;
    private final GetCitiesByDepartmentPort getCitiesByDepartmentPort;
    private final GetSitesByCityPort      getSitesByCityPort;
    private final GetSitesByDepartmentPort getSitesByDepartmentPort;
    private final GetNearbySitesPort      getNearbySitesPort;
    private final GetSitePort             getSitePort;
    private final CreateSitePort          createSitePort;
    private final UpdateSitePort          updateSitePort;

    @Operation(summary = "List all departments")
    @GetMapping("/departments")
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> getDepartments() {
        return ResponseEntity.ok(ApiResponse.ok("Departments retrieved successfully",
                getDepartmentsPort.getDepartments()));
    }

    @Operation(summary = "List cities by department")
    @GetMapping("/departments/{departmentId}/cities")
    public ResponseEntity<ApiResponse<List<CityResponse>>> getCitiesByDepartment(
            @PathVariable Integer departmentId) {
        return ResponseEntity.ok(ApiResponse.ok("Cities retrieved successfully",
                getCitiesByDepartmentPort.getCitiesByDepartment(departmentId)));
    }

    @Operation(summary = "List sites by city")
    @GetMapping("/cities/{cityId}/sites")
    public ResponseEntity<ApiResponse<List<SiteResponse>>> getSitesByCity(
            @PathVariable Integer cityId) {
        return ResponseEntity.ok(ApiResponse.ok("Sites retrieved successfully",
                getSitesByCityPort.getSitesByCity(cityId)));
    }

    @Operation(summary = "List sites by department")
    @GetMapping("/departments/{departmentId}/sites")
    public ResponseEntity<ApiResponse<List<SiteResponse>>> getSitesByDepartment(
            @PathVariable Integer departmentId) {
        return ResponseEntity.ok(ApiResponse.ok("Sites retrieved successfully",
                getSitesByDepartmentPort.getSitesByDepartment(departmentId)));
    }

    @Operation(summary = "List nearby sites by coordinates and radius")
    @GetMapping("/sites/nearby")
    public ResponseEntity<ApiResponse<List<SiteResponse>>> getNearbySites(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam Double radiusKm) {
        return ResponseEntity.ok(ApiResponse.ok("Nearby sites retrieved successfully",
                getNearbySitesPort.getNearbySites(
                        BigDecimal.valueOf(latitude),
                        BigDecimal.valueOf(longitude),
                        radiusKm)));
    }

    @Operation(summary = "Get site by ID")
    @GetMapping("/sites/{siteId}")
    public ResponseEntity<ApiResponse<SiteResponse>> getSite(
            @PathVariable Integer siteId) {
        return ResponseEntity.ok(ApiResponse.ok("Site retrieved successfully",
                getSitePort.getSite(siteId)));
    }

    @Operation(summary = "Create a new site (ADMIN or ORGANIZER)")
    @PostMapping("/sites")
    public ResponseEntity<ApiResponse<SiteResponse>> createSite(
            @Valid @RequestBody CreateSiteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Site created successfully",
                        createSitePort.createSite(request)));
    }

    @Operation(summary = "Update an existing site (ADMIN or ORGANIZER)")
    @PutMapping("/sites/{siteId}")
    public ResponseEntity<ApiResponse<SiteResponse>> updateSite(
            @PathVariable Integer siteId,
            @Valid @RequestBody UpdateSiteRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Site updated successfully",
                updateSitePort.updateSite(siteId, request)));
    }
}
