package com.capysoft.tuevento.modules.language.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO para crear un nuevo idioma.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateLanguageRequest {

    @NotBlank(message = "Language code is required")
    @Size(max = 10, message = "Language code must not exceed 10 characters")
    private String code;

    @NotBlank(message = "Language name is required")
    @Size(max = 50, message = "Language name must not exceed 50 characters")
    private String name;
}