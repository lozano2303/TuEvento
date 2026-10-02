package com.capysoft.tuevento.modules.language.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO para actualizar un idioma existente.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLanguageRequest {

    @NotBlank(message = "Language name is required")
    @Size(max = 50, message = "Language name must not exceed 50 characters")
    private String name;
}