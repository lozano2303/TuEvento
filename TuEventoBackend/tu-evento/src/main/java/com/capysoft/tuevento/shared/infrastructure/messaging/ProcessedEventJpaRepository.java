package com.capysoft.tuevento.shared.infrastructure.messaging;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventJpaRepository
        extends JpaRepository<ProcessedEventEntity, ProcessedEventEntity.Key> {
}
