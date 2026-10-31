package com.capysoft.commerce.shared.messaging;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventJpaRepository
        extends JpaRepository<ProcessedEventEntity, ProcessedEventEntity.Key> {
}
