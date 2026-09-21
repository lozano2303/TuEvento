package com.capysoft.tuevento.modules.ticket.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.ticket.domain.model.TicketCheckin;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketCheckinRepository;
import com.capysoft.tuevento.modules.ticket.infrastructure.persistence.entity.TicketCheckinEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Implementación del repositorio TicketCheckin usando JPA.
 */
@Component
@RequiredArgsConstructor
public class TicketCheckinRepositoryImpl implements TicketCheckinRepository {
    
    private final TicketCheckinJpaRepository jpaRepository;
    
    @Override
    public TicketCheckin save(TicketCheckin checkin) {
        TicketCheckinEntity entity = toEntity(checkin);
        TicketCheckinEntity savedEntity = jpaRepository.save(entity);
        return toDomain(savedEntity);
    }
    
    @Override
    public Optional<TicketCheckin> findByTicketId(Long ticketId) {
        return jpaRepository.findByTicketId(ticketId).map(this::toDomain);
    }
    
    @Override
    public boolean existsByTicketId(Long ticketId) {
        return jpaRepository.existsByTicketId(ticketId);
    }
    
    private TicketCheckinEntity toEntity(TicketCheckin checkin) {
        return TicketCheckinEntity.builder()
            .checkinId(checkin.getCheckinId())
            .ticketId(checkin.getTicketId())
            .checkinTime(checkin.getCheckinTime())
            .validatedBy(checkin.getValidatedBy())
            .build();
    }
    
    private TicketCheckin toDomain(TicketCheckinEntity entity) {
        return TicketCheckin.builder()
            .checkinId(entity.getCheckinId())
            .ticketId(entity.getTicketId())
            .checkinTime(entity.getCheckinTime())
            .validatedBy(entity.getValidatedBy())
            .build();
    }
}
