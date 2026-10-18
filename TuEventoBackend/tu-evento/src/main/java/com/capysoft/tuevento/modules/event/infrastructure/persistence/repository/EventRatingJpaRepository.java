package com.capysoft.tuevento.modules.event.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.event.infrastructure.persistence.entity.EventRatingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventRatingJpaRepository extends JpaRepository<EventRatingEntity, Long> {

    /** Lista todos los comentarios de un evento, ordenados más recientes primero. */
    List<EventRatingEntity> findByEventIdOrderByCreatedAtDescRatingIdDesc(Long eventId);

    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    /**
     * Último comentario del usuario en el evento, por fecha.
     * Usado por el antispam (R5) para comparar con now().
     */
    Optional<EventRatingEntity> findTop1ByEventIdAndUserIdOrderByCreatedAtDesc(Long eventId, Long userId);

    /**
     * ¿Tiene ya el usuario un comentario PRINCIPAL con rating != null en este evento?
     * Solo comentarios principales (parent_rating_id IS NULL) cuentan para R3.
     */
    @Query("SELECT COUNT(r) > 0 FROM EventRatingEntity r " +
           "WHERE r.eventId = :eventId AND r.userId = :userId " +
           "AND r.rating IS NOT NULL AND r.parentRatingId IS NULL")
    boolean existsRatedCommentByEventIdAndUserId(@Param("eventId") Long eventId,
                                                  @Param("userId")  Long userId);

    /** Respuestas directas de un comentario principal, orden cronológico. */
    List<EventRatingEntity> findByParentRatingIdOrderByCreatedAtAscRatingIdAsc(Long parentRatingId);

    /** Borra todas las respuestas directas de un comentario principal. */
    void deleteAllByParentRatingId(Long parentRatingId);
}
