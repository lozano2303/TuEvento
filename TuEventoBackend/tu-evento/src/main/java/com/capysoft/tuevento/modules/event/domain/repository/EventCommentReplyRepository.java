package com.capysoft.tuevento.modules.event.domain.repository;

import com.capysoft.tuevento.modules.event.domain.model.EventCommentReply;

import java.util.List;
import java.util.Optional;

public interface EventCommentReplyRepository {

    EventCommentReply save(EventCommentReply reply);
    List<EventCommentReply> findByRatingId(Long ratingId);
    Optional<EventCommentReply> findById(Long replyId);
}
