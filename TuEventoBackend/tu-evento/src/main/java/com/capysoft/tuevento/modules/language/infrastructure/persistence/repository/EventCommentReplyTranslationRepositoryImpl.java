package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.domain.model.EventCommentReplyTranslation;
import com.capysoft.tuevento.modules.language.domain.repository.EventCommentReplyTranslationRepository;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper.EventCommentReplyTranslationMapper;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
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
    public Optional<EventCommentReplyTranslation> findByReplyAndLanguage(Long replyId, Long languageId) {
        return jpaRepository.findByReplyIdAndLanguageId(replyId.intValue(), languageId.intValue())
                .map(mapper::toDomain);
    }

    @Override
    public List<EventCommentReplyTranslation> findByReply(Long replyId) {
        return jpaRepository.findByReplyId(replyId.intValue())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventCommentReplyTranslation> findByLanguage(Long languageId) {
        return jpaRepository.findByLanguageId(languageId.intValue())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventCommentReplyTranslation> findByStatus(TranslationStatus status) {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .filter(translation -> translation.getStatus().equals(status))
                .collect(Collectors.toList());
    }

    @Override
    public List<EventCommentReplyTranslation> findByReplyAndStatus(Long replyId, TranslationStatus status) {
        return jpaRepository.findByReplyId(replyId.intValue())
                .stream()
                .map(mapper::toDomain)
                .filter(translation -> translation.getStatus().equals(status))
                .collect(Collectors.toList());
    }

    @Override
    public EventCommentReplyTranslation save(EventCommentReplyTranslation translation) {
        var entity = mapper.toEntity(translation);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public List<EventCommentReplyTranslation> saveAll(List<EventCommentReplyTranslation> translations) {
        var entities = translations.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());
        var savedEntities = jpaRepository.saveAll(entities);
        return savedEntities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(EventCommentReplyTranslation translation) {
        var entity = mapper.toEntity(translation);
        jpaRepository.delete(entity);
    }

    @Override
    public void deleteByReply(Long replyId) {
        var entities = jpaRepository.findByReplyId(replyId.intValue());
        jpaRepository.deleteAll(entities);
    }

    @Override
    public boolean existsByReplyAndLanguage(Long replyId, Long languageId) {
        return jpaRepository.findByReplyIdAndLanguageId(replyId.intValue(), languageId.intValue())
                .isPresent();
    }
}