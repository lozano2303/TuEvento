package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.domain.model.EventCommentReplyTranslation;
import com.capysoft.tuevento.modules.language.domain.repository.EventCommentReplyTranslationRepository;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper.EventCommentReplyTranslationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class EventCommentReplyTranslationRepositoryImpl implements EventCommentReplyTranslationRepository {

    private final EventCommentReplyTranslationJpaRepository jpaRepository;
    private final EventCommentReplyTranslationMapper mapper;

    @Override
    public EventCommentReplyTranslation save(EventCommentReplyTranslation translation) {
        var entity = mapper.toEntity(translation);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<EventCommentReplyTranslation> findById(Integer id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<EventCommentReplyTranslation> findByReplyId(Integer replyId) {
        return jpaRepository.findByReplyId(replyId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<EventCommentReplyTranslation> findByReplyIdAndLanguageId(Integer replyId, Integer languageId) {
        return jpaRepository.findByReplyIdAndLanguageId(replyId, languageId)
                .map(mapper::toDomain);
    }

    @Override
    public List<EventCommentReplyTranslation> findByLanguageId(Integer languageId) {
        return jpaRepository.findByLanguageId(languageId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(EventCommentReplyTranslation translation) {
        var entity = mapper.toEntity(translation);
        jpaRepository.delete(entity);
    }

    @Override
    public void deleteById(Integer id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Integer id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public List<EventCommentReplyTranslation> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}