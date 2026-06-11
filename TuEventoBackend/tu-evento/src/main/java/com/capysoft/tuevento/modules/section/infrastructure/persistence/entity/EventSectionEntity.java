package com.capysoft.tuevento.modules.section.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "event_section")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventSectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_section_id")
    private Integer eventSectionId;

    @Column(name = "event_id", nullable = false)
    private Integer eventId;

    @Column(name = "section_type_id", nullable = false)
    private Integer sectionTypeId;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
