package com.capysoft.commerce.ticket.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.capysoft.commerce.ticket.infrastructure.persistence.entity.EventRefEntity;

public interface EventRefJpaRepository extends JpaRepository<EventRefEntity, Long> {

    List<EventRefEntity> findByOwnerId(Long ownerId);
}
