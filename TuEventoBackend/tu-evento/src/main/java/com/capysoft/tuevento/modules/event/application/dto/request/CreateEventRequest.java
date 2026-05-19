package com.capysoft.tuevento.modules.event.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEventRequest {

    @NotBlank
    private String eventName;

    @NotBlank
    private String description;

    @NotNull
    private Long siteId;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate finishDate;

    @Builder.Default
    private boolean isPublic = true;

    @Min(1)
    @Max(100000)
    private int availableSeats;

    @NotNull(message = "categoryId is required")
    private Integer categoryId;
}
