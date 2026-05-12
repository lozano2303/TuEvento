package com.capysoft.tuevento.modules.event.interfaces.rest;

import com.capysoft.tuevento.modules.event.application.dto.request.AddCommentReplyRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventCommentReplyResponse;
import com.capysoft.tuevento.modules.event.application.port.in.AddCommentReplyUseCase;
import com.capysoft.tuevento.modules.event.domain.repository.EventCommentReplyRepository;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityUser;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ratings/{ratingId}/replies")
@RequiredArgsConstructor
@Tag(name = "Event Comment Replies", description = "Comment reply endpoints")
public class EventCommentController {

    private final AddCommentReplyUseCase       addCommentReplyUseCase;
    private final EventCommentReplyRepository  commentReplyRepository;

    @Operation(summary = "Add a reply to a rating comment")
    @PostMapping
    public ResponseEntity<ApiResponse<EventCommentReplyResponse>> addReply(
            @PathVariable Long ratingId,
            @Valid @RequestBody AddCommentReplyRequest request,
            @AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Reply added successfully",
                        addCommentReplyUseCase.execute(ratingId, request, principal.getUserId().longValue())));
    }

    @Operation(summary = "Get replies for a rating")
    @GetMapping
    public ResponseEntity<ApiResponse<List<EventCommentReplyResponse>>> getReplies(
            @PathVariable Long ratingId) {
        List<EventCommentReplyResponse> replies = commentReplyRepository.findByRatingId(ratingId).stream()
                .map(r -> EventCommentReplyResponse.builder()
                        .replyId(r.getReplyId())
                        .parentReplyId(r.getParentReplyId())
                        .userId(r.getUserId())
                        .replyText(r.getReplyText())
                        .isVisible(r.isVisible())
                        .createdAt(r.getCreatedAt())
                        .build())
                .toList();
        return ResponseEntity.ok(ApiResponse.ok("Replies retrieved successfully", replies));
    }
}
