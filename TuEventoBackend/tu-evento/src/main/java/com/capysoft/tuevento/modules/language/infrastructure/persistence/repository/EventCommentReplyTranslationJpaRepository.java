package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.EventCommentReplyTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventCommentReplyTranslationJpaRepository extends JpaRepository<EventCommentReplyTranslationEntity, Integer> {
    
    List<EventCommentReplyTranslationEntity> findByReplyId(Integer replyId);
    
    Optional<EventCommentReplyTranslationEntity> findByReplyIdAndLanguageId(Integer replyId, Integer languageId);
    
    List<EventCommentReplyTranslationEntity> findByLanguageId(Integer languageId);
}