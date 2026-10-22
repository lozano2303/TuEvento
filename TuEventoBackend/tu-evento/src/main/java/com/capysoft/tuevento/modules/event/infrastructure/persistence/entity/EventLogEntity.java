package com.capysoft.tuevento.modules.event.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
}