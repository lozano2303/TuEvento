package com.capysoft.tuevento.modules.profile.domain.model;

import com.capysoft.tuevento.modules.geolocation.domain.model.City;
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
    private City city;
    private Integer storedFileId;
    private String fullName;
    private String bio;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
