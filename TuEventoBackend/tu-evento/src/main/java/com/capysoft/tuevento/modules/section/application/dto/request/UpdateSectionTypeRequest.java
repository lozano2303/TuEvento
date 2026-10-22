package com.capysoft.tuevento.modules.section.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class UpdateSectionTypeRequest {
    @NotBlank(message = "El nombre es requerido")
    private String name;
}
