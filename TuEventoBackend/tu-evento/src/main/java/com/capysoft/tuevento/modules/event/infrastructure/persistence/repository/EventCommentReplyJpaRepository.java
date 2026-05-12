package com.capysoft.tuevento.modules.event.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.event.infrastructure.persistence.entity.EventCommentReplyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventCommentReplyJpaRepository extends JpaRepository<EventCommentReplyEntity, Long> {

    List<EventCommentReplyEntity> findByRatingId(Long ratingId);
}
