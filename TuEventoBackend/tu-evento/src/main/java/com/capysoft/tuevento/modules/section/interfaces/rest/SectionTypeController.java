package com.capysoft.tuevento.modules.section.interfaces.rest;

import com.capysoft.tuevento.modules.section.application.dto.request.CreateSectionTypeRequest;
import com.capysoft.tuevento.modules.section.application.dto.response.SectionTypeResponse;
import com.capysoft.tuevento.modules.section.application.port.in.SectionTypeUseCase;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
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
public class SectionTypeController {

    private final SectionTypeUseCase sectionTypeUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SectionTypeResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok("Tipos de sección obtenidos correctamente",
                sectionTypeUseCase.getAllSectionTypes()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<SectionTypeResponse>> create(
            @Valid @RequestBody CreateSectionTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tipo de sección creado correctamente",
                        sectionTypeUseCase.createSectionType(request)));
    }
}
