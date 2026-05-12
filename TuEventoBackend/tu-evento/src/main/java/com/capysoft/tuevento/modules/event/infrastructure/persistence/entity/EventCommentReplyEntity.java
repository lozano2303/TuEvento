package com.capysoft.tuevento.modules.event.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "event_comment_reply")
public class EventCommentReplyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reply_id")
    private Long replyId;

    /** Null for root-level replies. */
    @Column(name = "parent_reply_id")
    private Long parentReplyId;

    @Column(name = "rating_id", nullable = false)
    private Long ratingId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "reply_text", nullable = false)
    private String replyText;

    @Column(name = "is_visible", nullable = false)
    private boolean isVisible;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
