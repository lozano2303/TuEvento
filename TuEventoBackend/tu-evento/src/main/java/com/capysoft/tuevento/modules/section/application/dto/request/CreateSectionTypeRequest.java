package com.capysoft.tuevento.modules.section.application.dto.request;

import com.capysoft.tuevento.modules.section.domain.model.SectionCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSectionTypeRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String name;

    /** Opcional: GENERAL si no se indica */
    private SectionCategory category;
}
