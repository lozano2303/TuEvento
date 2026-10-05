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

    /** Para la vista sigue existiendo, pero el service ya no la necesita para duplicados. */
    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    /**
     * Último comentario del usuario en el evento, por fecha.
     * Usado por el antispam (R5) para comparar con now().
     */
    Optional<EventRatingEntity> findTop1ByEventIdAndUserIdOrderByCreatedAtDesc(Long eventId, Long userId);

    /**
     * ¿Tiene ya el usuario un comentario con rating != null en este evento?
     * Usado para R3: determinar si el próximo comentario debe llevar calificación.
     */
    @Query("SELECT COUNT(r) > 0 FROM EventRatingEntity r " +
           "WHERE r.eventId = :eventId AND r.userId = :userId AND r.rating IS NOT NULL")
    boolean existsRatedCommentByEventIdAndUserId(@Param("eventId") Long eventId,
                                                  @Param("userId")  Long userId);
}
