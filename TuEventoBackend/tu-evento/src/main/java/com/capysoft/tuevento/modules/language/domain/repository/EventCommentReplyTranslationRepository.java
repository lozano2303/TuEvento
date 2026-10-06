package com.capysoft.tuevento.modules.language.domain.repository;

import com.capysoft.tuevento.modules.language.domain.model.EventCommentReplyTranslation;

import java.util.List;
import java.util.Optional;

public interface EventCommentReplyTranslationRepository {

    EventCommentReplyTranslation save(EventCommentReplyTranslation translation);

    Optional<EventCommentReplyTranslation> findById(Integer id);

    List<EventCommentReplyTranslation> findByReplyId(Integer replyId);

    Optional<EventCommentReplyTranslation> findByReplyIdAndLanguageId(Integer replyId, Integer languageId);

    List<EventCommentReplyTranslation> findByLanguageId(Integer languageId);

    void delete(EventCommentReplyTranslation translation);

    void deleteById(Integer id);

    boolean existsById(Integer id);

    List<EventCommentReplyTranslation> findAll();
}