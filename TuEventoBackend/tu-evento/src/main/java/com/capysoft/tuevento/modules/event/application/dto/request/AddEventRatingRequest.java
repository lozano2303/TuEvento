package com.capysoft.tuevento.modules.event.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddEventRatingRequest {

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @NotBlank
    private String comment;
}
