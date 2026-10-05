package com.capysoft.tuevento.modules.event.domain.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRatingDeletedEvent {

    private Long ratingId;
    private Long eventId;
    private Long userId;
    private LocalDateTime occurredAt;
}
