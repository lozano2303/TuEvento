package com.capysoft.tuevento.modules.event.domain.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRatingAddedEvent {

    private Long ratingId;
    private Long eventId;
    private Long userId;
    private int rating;
    private LocalDateTime occurredAt;
}
