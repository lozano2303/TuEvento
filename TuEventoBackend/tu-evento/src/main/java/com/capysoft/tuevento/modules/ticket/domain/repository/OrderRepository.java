package com.capysoft.tuevento.modules.ticket.domain.repository;

import java.util.List;
import java.util.Optional;

import com.capysoft.tuevento.modules.ticket.domain.model.Order;

/**
 * Repositorio del dominio para Order.
 */
public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(Long orderId);
    List<Order> findByUserId(Long userId);
    List<Order> findByEventId(Long eventId);
    boolean existsById(Long orderId);
}
