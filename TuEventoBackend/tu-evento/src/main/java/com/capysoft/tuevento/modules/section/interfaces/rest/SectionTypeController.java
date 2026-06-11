package com.capysoft.tuevento.modules.section.interfaces.rest;

import com.capysoft.tuevento.modules.section.application.dto.request.CreateSectionTypeRequest;
import com.capysoft.tuevento.modules.section.application.dto.response.SectionTypeResponse;
import com.capysoft.tuevento.modules.section.application.port.in.SectionTypeUseCase;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/section-types")
@RequiredArgsConstructor
@Tag(name = "Section Types", description = "Section type catalog endpoints")
public class SectionTypeController {

    private final SectionTypeUseCase sectionTypeUseCase;

    @Operation(summary = "List all section types — public")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SectionTypeResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok("Tipos de sección obtenidos correctamente",
                sectionTypeUseCase.getAllSectionTypes()));
    }

    @Operation(summary = "Create a new section type")
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<SectionTypeResponse>> create(
            @Valid @RequestBody CreateSectionTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tipo de sección creado correctamente",
                        sectionTypeUseCase.createSectionType(request)));
    }
}
