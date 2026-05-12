package com.capysoft.tuevento.modules.event.interfaces.rest;

import com.capysoft.tuevento.modules.event.application.dto.request.AddEventRatingRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventRatingResponse;
import com.capysoft.tuevento.modules.event.application.port.in.AddEventRatingUseCase;
import com.capysoft.tuevento.modules.event.domain.repository.EventRatingRepository;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityUser;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events/{eventId}/ratings")
@RequiredArgsConstructor
@Tag(name = "Event Ratings", description = "Event rating endpoints")
public class EventRatingController {

    private final AddEventRatingUseCase addEventRatingUseCase;
    private final EventRatingRepository eventRatingRepository;

    @Operation(summary = "Add a rating to an event")
    @PostMapping
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<ApiResponse<EventRatingResponse>> addRating(
            @PathVariable Long eventId,
            @Valid @RequestBody AddEventRatingRequest request,
            @AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Rating added successfully",
                        addEventRatingUseCase.execute(eventId, request, principal.getUserId().longValue())));
    }

    @Operation(summary = "Get ratings for an event")
    @GetMapping
    public ResponseEntity<ApiResponse<List<EventRatingResponse>>> getRatings(
            @PathVariable Long eventId) {
        List<EventRatingResponse> ratings = eventRatingRepository.findByEventId(eventId).stream()
                .map(r -> EventRatingResponse.builder()
                        .ratingId(r.getRatingId())
                        .userId(r.getUserId())
                        .rating(r.getRating())
                        .comment(r.getComment())
                        .isVisible(r.isVisible())
                        .createdAt(r.getCreatedAt())
                        .build())
                .toList();
        return ResponseEntity.ok(ApiResponse.ok("Ratings retrieved successfully", ratings));
    }
}
