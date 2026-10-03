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

    /**
     * Required when {@code newStatus} is {@code REJECTED}; must be non-blank.
     * Optional (ignored) for all other transitions.
     * Validation is enforced at the service layer, not here, so that the error
     * carries the project's standard {@code BusinessException} code.
     */
    private String reason;
}
