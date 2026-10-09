package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.NotificationTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTranslationJpaRepository extends JpaRepository<NotificationTranslationEntity, Integer> {

    @Query("SELECT nt FROM NotificationTranslationEntity nt WHERE nt.notificationId = :notificationId")
    List<NotificationTranslationEntity> findByNotificationId(@Param("notificationId") Long notificationId);

    @Query("SELECT nt FROM NotificationTranslationEntity nt WHERE nt.notificationId = :notificationId AND nt.languageId = :languageId")
    Optional<NotificationTranslationEntity> findByNotificationIdAndLanguageId(@Param("notificationId") Long notificationId, 
                                                                              @Param("languageId") Integer languageId);

    @Query("SELECT nt FROM NotificationTranslationEntity nt WHERE nt.languageId = :languageId")
    List<NotificationTranslationEntity> findByLanguageId(@Param("languageId") Integer languageId);

    @Query("SELECT nt FROM NotificationTranslationEntity nt WHERE nt.status = :status")
    List<NotificationTranslationEntity> findByStatus(@Param("status") String status);
}