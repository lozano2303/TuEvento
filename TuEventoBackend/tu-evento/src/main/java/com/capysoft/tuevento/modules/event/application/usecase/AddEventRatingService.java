package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.event.application.dto.request.AddEventRatingRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventRatingResponse;
import com.capysoft.tuevento.modules.event.application.port.in.AddEventRatingUseCase;
import com.capysoft.tuevento.modules.event.domain.event.EventRatingAddedEvent;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventRating;
import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.domain.repository.EventRatingRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AddEventRatingService implements AddEventRatingUseCase {

    private final EventRepository eventRepository;
    private final EventRatingRepository ratingRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public EventRatingResponse execute(Long eventId, AddEventRatingRequest request, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("EVENT_NOT_FOUND",
                        "Event not found with id: " + eventId));

        if (event.getStatus() != EventStatus.COMPLETED && event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessException("EVENT_RATING_NOT_ALLOWED",
                    "Ratings are only allowed for PUBLISHED or COMPLETED events");
        }

        if (ratingRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw new BusinessException("EVENT_ALREADY_RATED",
                    "User " + userId + " has already rated event " + eventId);
        }

        EventRating rating = ratingRepository.save(EventRating.builder()
                .eventId(eventId)
                .userId(userId)
                .rating(request.getRating())
                .comment(request.getComment())
                .isVisible(true)
                .createdAt(LocalDateTime.now())
                .build());

        eventPublisher.publishEvent(EventRatingAddedEvent.builder()
                .ratingId(rating.getRatingId())
                .eventId(eventId)
                .userId(userId)
                .rating(rating.getRating())
                .occurredAt(rating.getCreatedAt())
                .build());

        return EventRatingResponse.builder()
                .ratingId(rating.getRatingId())
                .userId(rating.getUserId())
                .rating(rating.getRating())
                .comment(rating.getComment())
                .isVisible(rating.isVisible())
                .createdAt(rating.getCreatedAt())
                .build();
    }
}
