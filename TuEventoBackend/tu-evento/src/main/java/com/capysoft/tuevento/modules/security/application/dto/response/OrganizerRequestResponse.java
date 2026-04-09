package com.capysoft.tuevento.modules.security.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizerRequestResponse {
    private Integer organizerPetitionId;
    private Integer userId;
    private String alias;
    private String status;
    private LocalDateTime applicationDate;
}
