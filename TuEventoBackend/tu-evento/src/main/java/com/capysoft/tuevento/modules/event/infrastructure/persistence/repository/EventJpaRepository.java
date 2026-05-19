package com.capysoft.tuevento.modules.event.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.infrastructure.persistence.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EventJpaRepository extends JpaRepository<EventEntity, Long> {

    List<EventEntity> findByUserId(Long userId);
    List<EventEntity> findBySiteId(Long siteId);
    List<EventEntity> findByStatus(EventStatus status);
    boolean existsByEventNameAndStartDateAndSiteId(String eventName, LocalDate startDate, Long siteId);

    /** All published public events. */
    List<EventEntity> findByStatusAndIsPublic(EventStatus status, Boolean isPublic);

    /** Published public events whose site belongs to the given city (native SQL to avoid cross-module JPQL). */
    @Query(value = """
            SELECT e.* FROM event e
            JOIN site s ON e.site_id = s.site_id
            WHERE e.status = 'PUBLISHED' AND e.is_public = true
              AND s.city_id = :cityId
            """, nativeQuery = true)
    List<EventEntity> findPublishedByCityId(@Param("cityId") Long cityId);

    /** Published public events assigned to the given category via category_event. */
    @Query(value = """
            SELECT e.* FROM event e
            JOIN category_event ce ON e.event_id = ce.event_id
            WHERE e.status = 'PUBLISHED' AND e.is_public = true
              AND ce.category_id = :categoryId
            """, nativeQuery = true)
    List<EventEntity> findPublishedByCategoryId(@Param("categoryId") Integer categoryId);

    /** Published public events whose startDate falls within [from, to]. */
    @Query(value = """
            SELECT e.* FROM event e
            WHERE e.status = 'PUBLISHED' AND e.is_public = true
              AND e.start_date >= :from AND e.start_date <= :to
            """, nativeQuery = true)
    List<EventEntity> findPublishedByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
