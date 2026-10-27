package com.capysoft.identity.profile.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Profile {

    private Long profileId;
    private Integer userId;
    private Integer cityId;
    private Integer storedFileId;
    private String fullName;
    private String bio;
    private Long preferredLanguageId;
    private LocalDateTime nameChangedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
