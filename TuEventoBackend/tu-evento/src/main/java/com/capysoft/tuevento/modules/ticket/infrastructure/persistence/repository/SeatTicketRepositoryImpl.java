package com.capysoft.tuevento.modules.ticket.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.ticket.domain.model.SeatTicket;
import com.capysoft.tuevento.modules.ticket.domain.repository.SeatTicketRepository;
import com.capysoft.tuevento.modules.ticket.infrastructure.persistence.entity.SeatTicketEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementación del repositorio SeatTicket usando JPA.
 */
@Component
@RequiredArgsConstructor
public class SeatTicketRepositoryImpl implements SeatTicketRepository {
    
    private final SeatTicketJpaRepository jpaRepository;
    
    @Override
    public SeatTicket save(SeatTicket seatTicket) {
        SeatTicketEntity entity = toEntity(seatTicket);
        SeatTicketEntity savedEntity = jpaRepository.save(entity);
        return toDomain(savedEntity);
    }
    
    @Override
    public List<SeatTicket> saveAll(List<SeatTicket> seatTickets) {
        List<SeatTicketEntity> entities = seatTickets.stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
        List<SeatTicketEntity> savedEntities = jpaRepository.saveAll(entities);
        return savedEntities.stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public Optional<SeatTicket> findById(Long seatTicketId) {
        return jpaRepository.findById(seatTicketId).map(this::toDomain);
    }
    
    @Override
    public List<SeatTicket> findByTicketId(Long ticketId) {
        return jpaRepository.findByTicketId(ticketId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public Optional<SeatTicket> findBySeatIdAndTicketId(Integer seatId, Long ticketId) {
        return jpaRepository.findBySeatIdAndTicketId(seatId, ticketId).map(this::toDomain);
    }
    
    private SeatTicketEntity toEntity(SeatTicket seatTicket) {
        return SeatTicketEntity.builder()
            .seatTicketId(seatTicket.getSeatTicketId())
            .seatId(seatTicket.getSeatId())
            .ticketId(seatTicket.getTicketId())
            .price(seatTicket.getPrice())
            .build();
    }
    
    private SeatTicket toDomain(SeatTicketEntity entity) {
        return SeatTicket.builder()
            .seatTicketId(entity.getSeatTicketId())
            .seatId(entity.getSeatId())
            .ticketId(entity.getTicketId())
            .price(entity.getPrice())
            .build();
    }
}
