package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.domain.model.NotificationTranslation;
import com.capysoft.tuevento.modules.language.domain.repository.NotificationTranslationRepository;
import com.capysoft.tuevento.modules.language.infrastructure.persistence.mapper.NotificationTranslationMapper;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class NotificationTranslationRepositoryImpl implements NotificationTranslationRepository {

    private final NotificationTranslationJpaRepository jpaRepository;
    private final NotificationTranslationMapper mapper;

    @Autowired
    public NotificationTranslationRepositoryImpl(NotificationTranslationJpaRepository jpaRepository, 
                                                NotificationTranslationMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<NotificationTranslation> findByNotificationAndLanguage(Long notificationId, Long languageId) {
        return jpaRepository.findByNotificationIdAndLanguageId(notificationId, languageId.intValue())
                .map(mapper::toDomain);
    }

    @Override
    public List<NotificationTranslation> findByNotification(Long notificationId) {
        return jpaRepository.findByNotificationId(notificationId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationTranslation> findByLanguage(Long languageId) {
        return jpaRepository.findByLanguageId(languageId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationTranslation> findByStatus(TranslationStatus status) {
        return jpaRepository.findByStatus(status.name().toLowerCase())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationTranslation> findByNotificationAndStatus(Long notificationId, TranslationStatus status) {
        // This method would need a custom query in JpaRepository, for now filtering in memory
        return findByNotification(notificationId)
                .stream()
                .filter(t -> t.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public NotificationTranslation save(NotificationTranslation notificationTranslation) {
        var entity = mapper.toEntity(notificationTranslation);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public List<NotificationTranslation> saveAll(List<NotificationTranslation> translations) {
        var entities = translations.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());
        var savedEntities = jpaRepository.saveAll(entities);
        return savedEntities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(NotificationTranslation translation) {
        var entity = mapper.toEntity(translation);
        jpaRepository.delete(entity);
    }

    @Override
    public void deleteByNotification(Long notificationId) {
        var entities = jpaRepository.findByNotificationId(notificationId);
        jpaRepository.deleteAll(entities);
    }

    @Override
    public boolean existsByNotificationAndLanguage(Long notificationId, Long languageId) {
        return jpaRepository.findByNotificationIdAndLanguageId(notificationId, languageId.intValue()).isPresent();
    }
}