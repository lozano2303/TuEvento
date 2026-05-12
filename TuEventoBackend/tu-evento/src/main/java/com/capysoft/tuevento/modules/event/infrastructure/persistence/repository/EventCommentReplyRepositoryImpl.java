package com.capysoft.tuevento.modules.event.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.event.application.mapper.EventCommentReplyEntityMapper;
import com.capysoft.tuevento.modules.event.domain.model.EventCommentReply;
import com.capysoft.tuevento.modules.event.domain.repository.EventCommentReplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EventCommentReplyRepositoryImpl implements EventCommentReplyRepository {

    private final EventCommentReplyJpaRepository jpaRepository;
    private final EventCommentReplyEntityMapper mapper;

    @Override
    public EventCommentReply save(EventCommentReply reply) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(reply)));
    }

    @Override
    public List<EventCommentReply> findByRatingId(Long ratingId) {
        return jpaRepository.findByRatingId(ratingId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<EventCommentReply> findById(Long replyId) {
        return jpaRepository.findById(replyId).map(mapper::toDomain);
    }
}
