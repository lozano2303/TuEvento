package com.capysoft.tuevento.modules.ticket.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.ticket.domain.model.Money;
import com.capysoft.tuevento.modules.ticket.domain.model.Ticket;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketRepository;
import com.capysoft.tuevento.modules.ticket.infrastructure.persistence.entity.TicketEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementación del repositorio Ticket usando JPA.
 */
@Component
@RequiredArgsConstructor
public class TicketRepositoryImpl implements TicketRepository {
    
    private final TicketJpaRepository jpaRepository;
    
    @Override
    public Ticket save(Ticket ticket) {
        TicketEntity entity = toEntity(ticket);
        TicketEntity savedEntity = jpaRepository.save(entity);
        return toDomain(savedEntity);
    }
    
    @Override
    public List<Ticket> saveAll(List<Ticket> tickets) {
        List<TicketEntity> entities = tickets.stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
        List<TicketEntity> savedEntities = jpaRepository.saveAll(entities);
        return savedEntities.stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public Optional<Ticket> findById(Long ticketId) {
        return jpaRepository.findById(ticketId).map(this::toDomain);
    }
    
    @Override
    public List<Ticket> findByOrderId(Long orderId) {
        return jpaRepository.findByOrderId(orderId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Ticket> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public Optional<Ticket> findByCode(String code) {
        return jpaRepository.findByCode(code).map(this::toDomain);
    }
    
    @Override
    public boolean existsById(Long ticketId) {
        return jpaRepository.existsById(ticketId);
    }
    
    private TicketEntity toEntity(Ticket ticket) {
        return TicketEntity.builder()
            .ticketId(ticket.getTicketId())
            .eventId(ticket.getEventId())
            .userId(ticket.getUserId())
            .orderId(ticket.getOrderId())
            .code(ticket.getCode())
            .qrCode(ticket.getQrCode())
            .status(ticket.getStatus())
            .expirationDate(ticket.getExpirationDate())
            .totalPrice(ticket.getTotalPrice().getAmount())
            .currency(ticket.getTotalPrice().getCurrency())
            .build();
    }
    
    private Ticket toDomain(TicketEntity entity) {
        return Ticket.builder()
            .ticketId(entity.getTicketId())
            .eventId(entity.getEventId())
            .userId(entity.getUserId())
            .orderId(entity.getOrderId())
            .code(entity.getCode())
            .qrCode(entity.getQrCode())
            .status(entity.getStatus())
            .expirationDate(entity.getExpirationDate())
            .totalPrice(new Money(entity.getTotalPrice(), entity.getCurrency()))
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }
}
