package com.capysoft.tuevento.modules.event.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveEventLayoutRequest {

    @NotBlank
    private String layoutData;
}
