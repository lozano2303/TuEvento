package com.capysoft.tuevento.modules.event.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.event.domain.model.EventStatus;
import com.capysoft.tuevento.modules.event.infrastructure.persistence.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EventJpaRepository extends JpaRepository<EventEntity, Long> {

    List<EventEntity> findByUserId(Long userId);
    List<EventEntity> findBySiteId(Long siteId);
    List<EventEntity> findByStatus(EventStatus status);
    boolean existsByEventNameAndStartDateAndSiteId(String eventName, LocalDate startDate, Long siteId);
}
