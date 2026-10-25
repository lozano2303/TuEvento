package com.capysoft.tuevento.modules.section.application.dto.request;

import com.capysoft.tuevento.modules.section.domain.model.SectionCategory;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class UpdateSectionTypeRequest {
    @NotBlank(message = "El nombre es requerido")
    private String name;

    /** Opcional: si no se indica se conserva la actual */
    private SectionCategory category;
}
