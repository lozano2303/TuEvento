package com.capysoft.tuevento.modules.event.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventCommentReply {

    private Long replyId;
    private Long parentReplyId;
    private Long ratingId;
    private Long userId;
    private String replyText;
    private Boolean isVisible;
    private LocalDateTime createdAt;
}
