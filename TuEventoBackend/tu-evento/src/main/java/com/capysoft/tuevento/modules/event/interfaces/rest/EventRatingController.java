package com.capysoft.tuevento.modules.event.interfaces.rest;

import com.capysoft.tuevento.modules.event.application.dto.request.AddEventRatingRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventRatingResponse;
import com.capysoft.tuevento.modules.event.application.port.in.AddEventRatingUseCase;
import com.capysoft.tuevento.modules.event.application.port.in.DeleteEventRatingUseCase;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.model.EventRating;
import com.capysoft.tuevento.modules.event.domain.repository.EventRatingRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.modules.profile.infrastructure.persistence.repository.ProfileJpaRepository;
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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Endpoints de comentarios/ratings de eventos.
 *
 * <ul>
 *   <li>GET    — público, sin autenticación.</li>
 *   <li>POST   — cualquier usuario autenticado (R1).</li>
 *   <li>DELETE — cualquier usuario autenticado; el service valida ownership.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/events/{eventId}/ratings")
@RequiredArgsConstructor
@Tag(name = "Event Ratings", description = "Event comment/rating endpoints")
public class EventRatingController {

    private final AddEventRatingUseCase    addEventRatingUseCase;
    private final DeleteEventRatingUseCase deleteEventRatingUseCase;
    private final EventRatingRepository    eventRatingRepository;
    private final EventRepository          eventRepository;
    private final ProfileJpaRepository     profileJpaRepository;

    @Operation(summary = "Add a comment/rating to an event — any authenticated user")
    @PostMapping
    public ResponseEntity<ApiResponse<EventRatingResponse>> addRating(
            @PathVariable Long eventId,
            @Valid @RequestBody AddEventRatingRequest request,
            @AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Rating added successfully",
                        addEventRatingUseCase.execute(eventId, request,
                                principal.getUserId().longValue())));
    }

    /**
     * Lista comentarios del evento ordenados más recientes primero.
     * Carga perfiles en una sola consulta (sin N+1).
     * Incluye {@code isOrganizer} para que el frontend muestre la etiqueta "Organizador".
     */
    @Operation(summary = "Get comments for an event — public")
    @GetMapping
    public ResponseEntity<ApiResponse<List<EventRatingResponse>>> getRatings(
            @PathVariable Long eventId) {

        List<EventRating> ratings = eventRatingRepository.findByEventIdOrderByCreatedAtDesc(eventId);

        if (ratings.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok("Ratings retrieved successfully", List.of()));
        }

        // Resolver userId del organizador del evento (para isOrganizer)
        Long organizerUserId = eventRepository.findById(eventId)
                .map(Event::getUserId)
                .orElse(null);

        // Cargar todos los perfiles en una sola consulta (sin N+1)
        List<Integer> userIds = ratings.stream()
                .map(r -> r.getUserId().intValue())
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, String> nameByUserId = profileJpaRepository.findAllByUserIdIn(userIds)
                .stream()
                .collect(Collectors.toMap(
                        p -> p.getUserId(),
                        p -> p.getFullName()));

        final Long finalOrganizerUserId = organizerUserId;
        List<EventRatingResponse> response = ratings.stream()
                .map(r -> EventRatingResponse.builder()
                        .ratingId(r.getRatingId())
                        .userId(r.getUserId())
                        .authorName(nameByUserId.getOrDefault(r.getUserId().intValue(), "Usuario"))
                        .rating(r.getRating())
                        .comment(r.getComment())
                        .isVisible(r.getIsVisible())
                        .isOrganizer(finalOrganizerUserId != null
                                && finalOrganizerUserId.equals(r.getUserId()))
                        .createdAt(r.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.ok("Ratings retrieved successfully", response));
    }

    @Operation(summary = "Delete own comment — any authenticated user")
    @DeleteMapping("/{ratingId}")
    public ResponseEntity<ApiResponse<Void>> deleteRating(
            @PathVariable Long eventId,
            @PathVariable Long ratingId,
            @AuthenticationPrincipal SecurityUser principal) {
        deleteEventRatingUseCase.execute(eventId, ratingId, principal.getUserId().longValue());
        return ResponseEntity.ok(ApiResponse.ok("Rating deleted successfully"));
    }
}
