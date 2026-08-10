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
    private String fullName;
    private String email;
    private String documentType;
    private String status;
    private LocalDateTime applicationDate;
    private Integer storedFileId;
    /** Presigned URL (60-min) of the user's profile picture. Null if the user
     *  has no profile or their avatar is the default (stored_file_id = 1). */
    private String profilePicture;
}
