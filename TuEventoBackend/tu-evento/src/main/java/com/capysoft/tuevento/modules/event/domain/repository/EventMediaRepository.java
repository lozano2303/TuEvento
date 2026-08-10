package com.capysoft.tuevento.modules.event.domain.repository;

import com.capysoft.tuevento.modules.event.domain.model.EventMedia;

import java.util.List;
import java.util.Optional;

public interface EventMediaRepository {

    EventMedia save(EventMedia media);
    List<EventMedia> findByEventId(Long eventId);
    Optional<EventMedia> findById(Long mediaId);
    void delete(Long mediaId);

    /** Cuenta imágenes de un evento — para validar que haya al menos una antes de publicar. */
    long countByEventId(Long eventId);

    /** Primera imagen de un evento (mediaId más bajo) — para coverUrl en listado público. */
    Optional<EventMedia> findFirstByEventId(Long eventId);
}
