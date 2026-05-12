package com.capysoft.tuevento.modules.event.application.dto.request;

import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeEventStatusRequest {

    @NotNull
    private EventStatus newStatus;
}
