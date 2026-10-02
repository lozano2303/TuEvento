package com.capysoft.tuevento.modules.ticket.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.capysoft.tuevento.modules.ticket.infrastructure.persistence.entity.OrderEntity;

/**
 * Repositorio JPA para OrderEntity.
 */
@Repository
public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByUserId(Long userId);
    List<OrderEntity> findByEventId(Long eventId);
}
