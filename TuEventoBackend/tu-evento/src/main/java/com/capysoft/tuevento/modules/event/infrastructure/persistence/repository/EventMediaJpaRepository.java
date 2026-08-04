package com.capysoft.tuevento.modules.event.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.event.infrastructure.persistence.entity.EventMediaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventMediaJpaRepository extends JpaRepository<EventMediaEntity, Long> {

    List<EventMediaEntity> findByEventId(Long eventId);

    /** Cuenta cuántas imágenes tiene un evento — usado para validar publicación. */
    long countByEventId(Long eventId);

    /** Primera imagen de un evento (media_id más bajo) — para coverUrl en listado. */
    java.util.Optional<EventMediaEntity> findFirstByEventIdOrderByMediaIdAsc(Long eventId);
}
