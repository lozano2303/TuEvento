package com.capysoft.tuevento.modules.event.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.infrastructure.persistence.entity.EventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EventJpaRepository extends JpaRepository<EventEntity, Long> {

    List<EventEntity> findByUserId(Long userId);
    List<EventEntity> findBySiteId(Long siteId);
    List<EventEntity> findByStatus(EventStatus status);
    long countByStatus(EventStatus status);
    boolean existsByEventNameAndStartDateAndSiteId(String eventName, LocalDate startDate, Long siteId);

    /** All published public events. */
    List<EventEntity> findByStatusAndIsPublic(EventStatus status, Boolean isPublic);

    /**
     * All published public events that are still active today (finish_date >= today).
     * Used by the public listing endpoint — avoids showing expired events before
     * the auto-completion scheduler has had a chance to run.
     */
    @Query(value = """
            SELECT e.* FROM event e
            WHERE e.status = 'PUBLISHED' AND e.is_public = true
              AND e.finish_date >= :today
            """, nativeQuery = true)
    List<EventEntity> findActivePublished(@Param("today") LocalDate today);

    /** Published public events whose site belongs to the given city (native SQL to avoid cross-module JPQL). */
    @Query(value = """
            SELECT e.* FROM event e
            JOIN site s ON e.site_id = s.site_id
            WHERE e.status = 'PUBLISHED' AND e.is_public = true
              AND e.finish_date >= :today
              AND s.city_id = :cityId
            """, nativeQuery = true)
    List<EventEntity> findPublishedByCityId(@Param("today") LocalDate today,
                                            @Param("cityId") Long cityId);

    /** Published public events assigned to the given category via category_event. */
    @Query(value = """
            SELECT e.* FROM event e
            JOIN category_event ce ON e.event_id = ce.event_id
            WHERE e.status = 'PUBLISHED' AND e.is_public = true
              AND e.finish_date >= :today
              AND ce.category_id = :categoryId
            """, nativeQuery = true)
    List<EventEntity> findPublishedByCategoryId(@Param("today") LocalDate today,
                                                @Param("categoryId") Integer categoryId);

    /** Published public events whose startDate falls within [from, to]. */
    @Query(value = """
            SELECT e.* FROM event e
            WHERE e.status = 'PUBLISHED' AND e.is_public = true
              AND e.finish_date >= :today
              AND e.start_date >= :from AND e.start_date <= :to
            """, nativeQuery = true)
    List<EventEntity> findPublishedByDateRange(@Param("today") LocalDate today,
                                               @Param("from") LocalDate from,
                                               @Param("to") LocalDate to);

    /**
     * Full-text search on published active events — no date filter.
     * Used when the user types a query but picks no date.
     */
    @Query(value = """
            SELECT e.* FROM event e
            WHERE e.status = 'PUBLISHED' AND e.is_public = true
              AND e.finish_date >= :today
              AND LOWER(e.event_name) LIKE LOWER(CONCAT('%', :q, '%'))
            """, nativeQuery = true)
    List<EventEntity> searchPublishedByText(@Param("today") LocalDate today,
                                            @Param("q") String q);

    /**
     * Full-text + exact start_date search on published active events.
     * Used when the user selects a specific date. Both params are always non-null.
     */
    @Query(value = """
            SELECT e.* FROM event e
            WHERE e.status = 'PUBLISHED' AND e.is_public = true
              AND e.finish_date >= :today
              AND LOWER(e.event_name) LIKE LOWER(CONCAT('%', :q, '%'))
              AND e.start_date = :date
            """, nativeQuery = true)
    List<EventEntity> searchPublishedByTextAndDate(@Param("today") LocalDate today,
                                                   @Param("q") String q,
                                                   @Param("date") LocalDate date);

    /** PUBLISHED events (any visibility) whose finish_date < :date — used by auto-completion scheduler. */
    List<EventEntity> findByStatusAndFinishDateBefore(EventStatus status, LocalDate date);

    /** Most recent N events ordered by updatedAt — used for admin activity feed. */
    @Query("SELECT e FROM EventEntity e ORDER BY e.updatedAt DESC")
    List<EventEntity> findTopByOrderByUpdatedAtDesc(Pageable pageable);
}
