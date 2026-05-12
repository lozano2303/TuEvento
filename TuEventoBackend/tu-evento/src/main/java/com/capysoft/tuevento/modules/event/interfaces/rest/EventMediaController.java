package com.capysoft.tuevento.modules.event.interfaces.rest;

import com.capysoft.tuevento.modules.event.application.dto.request.UploadEventMediaRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventMediaResponse;
import com.capysoft.tuevento.modules.event.application.port.in.UploadEventMediaUseCase;
import com.capysoft.tuevento.modules.event.domain.repository.EventMediaRepository;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityUser;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events/{eventId}/media")
@RequiredArgsConstructor
@Tag(name = "Event Media", description = "Event media upload endpoints")
public class EventMediaController {

    private final UploadEventMediaUseCase uploadEventMediaUseCase;
    private final EventMediaRepository    eventMediaRepository;

    @Operation(summary = "Upload media for an event")
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('ORGANIZER')")
    public ResponseEntity<ApiResponse<EventMediaResponse>> uploadMedia(
            @PathVariable Long eventId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal SecurityUser principal) {
        UploadEventMediaRequest request = UploadEventMediaRequest.builder()
                .eventId(eventId)
                .file(file)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Media uploaded successfully",
                        uploadEventMediaUseCase.execute(request, principal.getUserId().longValue())));
    }

    @Operation(summary = "Get media for an event")
    @GetMapping
    public ResponseEntity<ApiResponse<List<EventMediaResponse>>> getMedia(
            @PathVariable Long eventId) {
        List<EventMediaResponse> media = eventMediaRepository.findByEventId(eventId).stream()
                .map(m -> EventMediaResponse.builder()
                        .mediaId(m.getMediaId())
                        .eventId(m.getEventId())
                        .imgUrl(m.getImgUrl())
                        .build())
                .toList();
        return ResponseEntity.ok(ApiResponse.ok("Media retrieved successfully", media));
    }
}
