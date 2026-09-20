package com.capysoft.tuevento.modules.ticket.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.ticket.infrastructure.persistence.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para OrderEntity.
 */
@Repository
public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByUserId(Long userId);
}
