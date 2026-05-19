package com.capysoft.tuevento.modules.event.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventRequest {

    /** Optional — if provided must not be blank and must respect length limits. */
    @Size(min = 1, max = 100)
    private String eventName;

    /** Optional — if provided must not be blank and must respect length limits. */
    @Size(min = 1, max = 255)
    private String description;

    /** Optional — if provided must be a positive ID. */
    @Positive
    private Long siteId;

    private LocalDate startDate;

    private LocalDate finishDate;

    private Boolean isPublic;

    /** Optional — if provided must be between 1 and 100000. */
    @Min(1)
    @Max(100000)
    private Integer availableSeats;

    /** Optional — if provided must be a positive category ID. */
    @Positive
    private Integer categoryId;
}
