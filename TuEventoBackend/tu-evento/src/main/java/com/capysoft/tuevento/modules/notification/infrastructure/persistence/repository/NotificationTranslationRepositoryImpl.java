package com.capysoft.tuevento.modules.notification.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.notification.domain.model.NotificationTranslation;
import com.capysoft.tuevento.modules.notification.domain.repository.NotificationTranslationRepository;
import com.capysoft.tuevento.modules.notification.infrastructure.persistence.mapper.NotificationTranslationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotificationTranslationRepositoryImpl implements NotificationTranslationRepository {

    private final NotificationTranslationJpaRepository jpaRepository;
    private final NotificationTranslationMapper mapper;

    @Override
    public Optional<NotificationTranslation> findByNotificationAndLanguage(Integer notificationId, Integer languageId) {
        return jpaRepository.findByNotificationIdAndLanguageId(notificationId, languageId).map(mapper::toDomain);
    }

    @Override
    public List<NotificationTranslation> findAllByNotificationId(Integer notificationId) {
        return jpaRepository.findByNotificationId(notificationId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public NotificationTranslation save(NotificationTranslation translation) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(translation)));
    }
}
