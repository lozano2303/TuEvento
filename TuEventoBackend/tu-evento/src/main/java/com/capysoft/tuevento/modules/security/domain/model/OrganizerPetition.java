package com.capysoft.tuevento.modules.security.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizerPetition {

    private Integer organizerPetitionId;
    private User user;
    private Integer storedFileId;
    private LocalDateTime applicationDate;
    private String status;
}
