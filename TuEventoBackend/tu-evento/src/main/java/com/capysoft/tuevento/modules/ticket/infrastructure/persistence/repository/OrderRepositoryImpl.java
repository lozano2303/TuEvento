package com.capysoft.tuevento.modules.ticket.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.ticket.domain.model.Money;
import com.capysoft.tuevento.modules.ticket.domain.model.Order;
import com.capysoft.tuevento.modules.ticket.domain.repository.OrderRepository;
import com.capysoft.tuevento.modules.ticket.infrastructure.persistence.entity.OrderEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementación del repositorio Order usando JPA.
 */
@Component
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {
    
    private final OrderJpaRepository jpaRepository;
    
    @Override
    public Order save(Order order) {
        OrderEntity entity = toEntity(order);
        OrderEntity savedEntity = jpaRepository.save(entity);
        return toDomain(savedEntity);
    }
    
    @Override
    public Optional<Order> findById(Long orderId) {
        return jpaRepository.findById(orderId).map(this::toDomain);
    }
    
    @Override
    public List<Order> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public boolean existsById(Long orderId) {
        return jpaRepository.existsById(orderId);
    }
    
    private OrderEntity toEntity(Order order) {
        return OrderEntity.builder()
            .orderId(order.getOrderId())
            .userId(order.getUserId())
            .eventId(order.getEventId())
            .totalAmount(order.getTotalAmount().getAmount())
            .currency(order.getTotalAmount().getCurrency())
            .status(order.getStatus())
            .build();
    }
    
    private Order toDomain(OrderEntity entity) {
        return Order.builder()
            .orderId(entity.getOrderId())
            .userId(entity.getUserId())
            .eventId(entity.getEventId())
            .totalAmount(new Money(entity.getTotalAmount(), entity.getCurrency()))
            .status(entity.getStatus())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }
}
