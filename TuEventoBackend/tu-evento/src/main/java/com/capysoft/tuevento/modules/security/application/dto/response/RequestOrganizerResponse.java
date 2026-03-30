package com.capysoft.tuevento.modules.security.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RequestOrganizerResponse {

    private final Integer organizerPetitionId;
    private final String status;
    private final LocalDateTime applicationDate;
}
