package com.capysoft.tuevento.modules.language.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "event_comment_reply_translation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventCommentReplyTranslationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "translation_id")
    private Integer translationId;

    @Column(name = "reply_id", nullable = false)
    private Integer replyId;

    @Column(name = "language_id", nullable = false)
    private Integer languageId;

    @Column(name = "translated_reply_text", length = 255)
    private String translatedReplyText;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TranslationStatusEnum status = TranslationStatusEnum.draft;
}