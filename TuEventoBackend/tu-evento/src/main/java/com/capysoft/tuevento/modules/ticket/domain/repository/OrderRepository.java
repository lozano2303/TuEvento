package com.capysoft.tuevento.modules.ticket.domain.repository;

import com.capysoft.tuevento.modules.ticket.domain.model.Order;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio del dominio para Order.
 */
public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(Long orderId);
    List<Order> findByUserId(Long userId);
    boolean existsById(Long orderId);
}
