package com.capysoft.tuevento.modules.ticket.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.ticket.domain.model.TicketLog;
import com.capysoft.tuevento.modules.ticket.domain.repository.TicketLogRepository;
import com.capysoft.tuevento.modules.ticket.infrastructure.persistence.entity.TicketLogEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación del repositorio TicketLog usando JPA.
 */
@Component
@RequiredArgsConstructor
public class TicketLogRepositoryImpl implements TicketLogRepository {
    
    private final TicketLogJpaRepository jpaRepository;
    
    @Override
    public TicketLog save(TicketLog log) {
        TicketLogEntity entity = toEntity(log);
        TicketLogEntity savedEntity = jpaRepository.save(entity);
        return toDomain(savedEntity);
    }
    
    @Override
    public List<TicketLog> findByTicketId(Long ticketId) {
        return jpaRepository.findByTicketId(ticketId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    private TicketLogEntity toEntity(TicketLog log) {
        return TicketLogEntity.builder()
            .ticketLogId(log.getTicketLogId())
            .ticketId(log.getTicketId())
            .oldStatus(log.getOldStatus())
            .newStatus(log.getNewStatus())
            .changedAt(log.getChangedAt())
            .changedBy(log.getChangedBy())
            .reason(log.getReason())
            .build();
    }
    
    private TicketLog toDomain(TicketLogEntity entity) {
        return TicketLog.builder()
            .ticketLogId(entity.getTicketLogId())
            .ticketId(entity.getTicketId())
            .oldStatus(entity.getOldStatus())
            .newStatus(entity.getNewStatus())
            .changedAt(entity.getChangedAt())
            .changedBy(entity.getChangedBy())
            .reason(entity.getReason())
            .build();
    }
}
