package com.capysoft.tuevento.modules.event.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EventRatingResponse {

    private final Long ratingId;
    private final Long userId;
    private final int rating;
    private final String comment;
    private final Boolean isVisible;
    private final LocalDateTime createdAt;
}
