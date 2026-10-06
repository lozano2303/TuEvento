package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.domain.model.NotificationTypeTranslation;
import com.capysoft.tuevento.modules.language.domain.repository.NotificationTypeTranslationRepository;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper.NotificationTypeTranslationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class NotificationTypeTranslationRepositoryImpl implements NotificationTypeTranslationRepository {

    private final NotificationTypeTranslationJpaRepository jpaRepository;
    private final NotificationTypeTranslationMapper mapper;

    @Override
    public NotificationTypeTranslation save(NotificationTypeTranslation translation) {
        var entity = mapper.toEntity(translation);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<NotificationTypeTranslation> findById(Integer id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<NotificationTypeTranslation> findByNotificationTypeId(Integer notificationTypeId) {
        return jpaRepository.findByNotificationTypeId(notificationTypeId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<NotificationTypeTranslation> findByNotificationTypeIdAndLanguageId(Integer notificationTypeId, Integer languageId) {
        return jpaRepository.findByNotificationTypeIdAndLanguageId(notificationTypeId, languageId)
                .map(mapper::toDomain);
    }

    @Override
    public List<NotificationTypeTranslation> findByLanguageId(Integer languageId) {
        return jpaRepository.findByLanguageId(languageId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(NotificationTypeTranslation translation) {
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
    public List<NotificationTypeTranslation> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}