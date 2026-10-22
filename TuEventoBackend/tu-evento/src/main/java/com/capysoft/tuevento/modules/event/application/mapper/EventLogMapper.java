package com.capysoft.tuevento.modules.event.application.mapper;

import com.capysoft.tuevento.modules.event.domain.model.EventLog;
import com.capysoft.tuevento.modules.event.infrastructure.persistence.entity.EventLogEntity;
import org.springframework.stereotype.Component;

@Component
public class EventLogMapper {

    public EventLogEntity toEntity(EventLog domain) {
        if (domain == null) return null;
        
        return EventLogEntity.builder()
                .logId(domain.getLogId())
                .eventId(domain.getEventId())
                .action(domain.getAction())
                .oldValue(domain.getOldValue())
                .newValue(domain.getNewValue())
                .occurredAt(domain.getOccurredAt())
                .build();
    }

    public EventLog toDomain(EventLogEntity entity) {
        if (entity == null) return null;
        
        return EventLog.builder()
                .logId(entity.getLogId())
                .eventId(entity.getEventId())
                .action(entity.getAction())
                .oldValue(entity.getOldValue())
                .newValue(entity.getNewValue())
                .occurredAt(entity.getOccurredAt())
                .build();
    }
}