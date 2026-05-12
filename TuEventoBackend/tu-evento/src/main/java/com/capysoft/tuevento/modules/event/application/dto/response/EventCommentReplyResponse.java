package com.capysoft.tuevento.modules.event.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EventCommentReplyResponse {

    private final Long replyId;
    private final Long parentReplyId;
    private final Long userId;
    private final String replyText;
    private final Boolean isVisible;
    private final LocalDateTime createdAt;
}
