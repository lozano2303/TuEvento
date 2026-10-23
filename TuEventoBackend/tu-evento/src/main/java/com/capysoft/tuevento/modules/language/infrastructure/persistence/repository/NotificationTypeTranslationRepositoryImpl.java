package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.domain.model.NotificationTypeTranslation;
import com.capysoft.tuevento.modules.language.domain.repository.NotificationTypeTranslationRepository;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper.NotificationTypeTranslationMapper;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
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
    public Optional<NotificationTypeTranslation> findByNotificationTypeAndLanguage(Long notificationTypeId, Integer languageId) {
        return jpaRepository.findByNotificationTypeIdAndLanguageId(notificationTypeId.intValue(), languageId)
                .map(mapper::toDomain);
    }

    @Override
    public List<NotificationTypeTranslation> findByNotificationType(Long notificationTypeId) {
        return jpaRepository.findByNotificationTypeId(notificationTypeId.intValue())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationTypeTranslation> findByLanguage(Integer languageId) {
        return jpaRepository.findByLanguageId(languageId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationTypeTranslation> findByStatus(TranslationStatus status) {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .filter(translation -> translation.getStatus().equals(status))
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationTypeTranslation> findByNotificationTypeAndStatus(Long notificationTypeId, TranslationStatus status) {
        return jpaRepository.findByNotificationTypeId(notificationTypeId.intValue())
                .stream()
                .map(mapper::toDomain)
                .filter(translation -> translation.getStatus().equals(status))
                .collect(Collectors.toList());
    }

    @Override
    public NotificationTypeTranslation save(NotificationTypeTranslation translation) {
        var entity = mapper.toEntity(translation);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public List<NotificationTypeTranslation> saveAll(List<NotificationTypeTranslation> translations) {
        var entities = translations.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());
        var savedEntities = jpaRepository.saveAll(entities);
        return savedEntities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(NotificationTypeTranslation translation) {
        var entity = mapper.toEntity(translation);
        jpaRepository.delete(entity);
    }

    @Override
    public void deleteByNotificationType(Long notificationTypeId) {
        var entities = jpaRepository.findByNotificationTypeId(notificationTypeId.intValue());
        jpaRepository.deleteAll(entities);
    }

    @Override
    public boolean existsByNotificationTypeAndLanguage(Long notificationTypeId, Integer languageId) {
        return jpaRepository.findByNotificationTypeIdAndLanguageId(notificationTypeId.intValue(), languageId)
                .isPresent();
    }
}