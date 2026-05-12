package com.capysoft.tuevento.modules.event.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRating {

    private Long ratingId;
    private Long eventId;
    private Long userId;
    private int rating;
    private String comment;
    private Boolean isVisible;
    private LocalDateTime createdAt;
}
