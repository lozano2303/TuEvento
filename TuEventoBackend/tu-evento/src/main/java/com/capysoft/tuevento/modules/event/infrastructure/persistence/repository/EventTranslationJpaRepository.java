package com.capysoft.tuevento.modules.event.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.event.infrastructure.persistence.entity.EventTranslationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventTranslationJpaRepository extends JpaRepository<EventTranslationEntity, Integer> {

    List<EventTranslationEntity> findByEventId(Integer eventId);

    Optional<EventTranslationEntity> findByEventIdAndLanguageId(Integer eventId, Integer languageId);

    @Modifying
    @Query("DELETE FROM EventTranslationEntity et WHERE et.eventId = :eventId")
    void deleteByEventId(@Param("eventId") Integer eventId);
}
