package com.capysoft.tuevento.modules.language.infrastructure.persistence.jpa;

import com.capysoft.tuevento.modules.language.infrastructure.persistence.entity.EventTranslationEntity;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para EventTranslationEntity.
 */
@Repository
public interface EventTranslationJpaRepository extends JpaRepository<EventTranslationEntity, Long> {

    /**
     * Busca traducción por evento e idioma.
     */
    @Query("SELECT et FROM EventTranslationEntity et WHERE et.eventId = :eventId AND et.language.languageId = :languageId")
    Optional<EventTranslationEntity> findByEventIdAndLanguageId(@Param("eventId") Long eventId, 
                                                               @Param("languageId") Long languageId);

    /**
     * Busca todas las traducciones de un evento.
     */
    List<EventTranslationEntity> findByEventId(Long eventId);

    /**
     * Busca todas las traducciones en un idioma.
     */
    @Query("SELECT et FROM EventTranslationEntity et WHERE et.language.languageId = :languageId")
    List<EventTranslationEntity> findByLanguageId(@Param("languageId") Long languageId);

    /**
     * Busca traducciones por estado.
     */
    List<EventTranslationEntity> findByStatus(TranslationStatus status);

    /**
     * Busca traducciones por evento y estado.
     */
    List<EventTranslationEntity> findByEventIdAndStatus(Long eventId, TranslationStatus status);

    /**
     * Elimina traducciones por evento.
     */
    void deleteByEventId(Long eventId);

    /**
     * Verifica existencia por evento e idioma.
     */
    @Query("SELECT COUNT(et) > 0 FROM EventTranslationEntity et WHERE et.eventId = :eventId AND et.language.languageId = :languageId")
    boolean existsByEventIdAndLanguageId(@Param("eventId") Long eventId, 
                                       @Param("languageId") Long languageId);
}