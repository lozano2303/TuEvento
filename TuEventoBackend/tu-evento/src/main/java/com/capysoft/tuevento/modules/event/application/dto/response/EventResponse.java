package com.capysoft.tuevento.modules.event.application.dto.response;

import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class EventResponse {

    private final Long eventId;
    private final Long userId;
    private final Long siteId;
    private final String siteName;
    private final String eventName;
    private final String description;
    private final LocalDate startDate;
    private final LocalDate finishDate;
    private final EventStatus status;
    private final Boolean isPublic;
    private final int availableSeats;
    private final Integer categoryId;
    
    // Translation fields - populated if language parameter is provided
    private final String translatedName;
    private final String translatedDescription;
    private final String languageCode;
    
    /** Indicates if translation is being generated (lazy loading) */
    private final Boolean translationGenerating;
    
    /** Populated when status is REJECTED; null otherwise. */
    private final String rejectionReason;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final String createdBy;
    private final String updatedBy;
}
