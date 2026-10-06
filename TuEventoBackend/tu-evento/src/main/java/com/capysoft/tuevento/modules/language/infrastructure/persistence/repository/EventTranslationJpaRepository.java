package com.capysoft.tuevento.modules.language.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.EventTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventTranslationJpaRepository extends JpaRepository<EventTranslationEntity, Long> {

    @Query("SELECT et FROM EventTranslationEntity et WHERE et.eventId = :eventId")
    List<EventTranslationEntity> findByEventId(@Param("eventId") Long eventId);

    @Query("SELECT et FROM EventTranslationEntity et WHERE et.eventId = :eventId AND et.language.languageId = :languageId")
    Optional<EventTranslationEntity> findByEventIdAndLanguageId(@Param("eventId") Long eventId, 
                                                               @Param("languageId") Long languageId);

    @Query("SELECT et FROM EventTranslationEntity et WHERE et.language.languageId = :languageId")
    List<EventTranslationEntity> findByLanguageId(@Param("languageId") Long languageId);

    @Query("SELECT et FROM EventTranslationEntity et WHERE et.status = :status")
    List<EventTranslationEntity> findByStatus(@Param("status") String status);

    @Query("SELECT et FROM EventTranslationEntity et WHERE et.source = :source")
    List<EventTranslationEntity> findBySource(@Param("source") String source);

    @Query("SELECT et FROM EventTranslationEntity et WHERE et.eventId = :eventId AND et.status = :status")
    List<EventTranslationEntity> findByEventIdAndStatus(@Param("eventId") Long eventId, @Param("status") String status);

    @Modifying
    @Query("DELETE FROM EventTranslationEntity et WHERE et.eventId = :eventId")
    void deleteByEventId(@Param("eventId") Long eventId);

    @Query("SELECT COUNT(et) > 0 FROM EventTranslationEntity et WHERE et.eventId = :eventId AND et.language.languageId = :languageId")
    boolean existsByEventIdAndLanguageId(@Param("eventId") Long eventId, @Param("languageId") Long languageId);
}