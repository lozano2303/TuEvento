package com.capysoft.tuevento.modules.event.application.usecase;

import com.capysoft.tuevento.modules.event.application.dto.request.AddCommentReplyRequest;
import com.capysoft.tuevento.modules.event.application.dto.response.EventCommentReplyResponse;
import com.capysoft.tuevento.modules.event.application.port.in.AddCommentReplyUseCase;
import com.capysoft.tuevento.modules.event.domain.model.EventCommentReply;
import com.capysoft.tuevento.modules.event.domain.repository.EventCommentReplyRepository;
import com.capysoft.tuevento.modules.event.domain.repository.EventRatingRepository;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AddCommentReplyService implements AddCommentReplyUseCase {

    private final EventRatingRepository ratingRepository;
    private final EventCommentReplyRepository replyRepository;

    @Override
    @Transactional
    public EventCommentReplyResponse execute(Long ratingId, AddCommentReplyRequest request, Long userId) {
        ratingRepository.findById(ratingId)
                .orElseThrow(() -> new NotFoundException("EVENT_RATING_NOT_FOUND",
                        "Rating not found with id: " + ratingId));

        if (request.getParentReplyId() != null) {
            replyRepository.findById(request.getParentReplyId())
                    .orElseThrow(() -> new NotFoundException("EVENT_REPLY_NOT_FOUND",
                            "Parent reply not found with id: " + request.getParentReplyId()));
        }

        EventCommentReply reply = replyRepository.save(EventCommentReply.builder()
                .ratingId(ratingId)
                .parentReplyId(request.getParentReplyId())
                .userId(userId)
                .replyText(request.getReplyText())
                .isVisible(true)
                .createdAt(LocalDateTime.now())
                .build());

        return EventCommentReplyResponse.builder()
                .replyId(reply.getReplyId())
                .parentReplyId(reply.getParentReplyId())
                .userId(reply.getUserId())
                .replyText(reply.getReplyText())
                .isVisible(reply.getIsVisible())
                .createdAt(reply.getCreatedAt())
                .build();
    }
}
