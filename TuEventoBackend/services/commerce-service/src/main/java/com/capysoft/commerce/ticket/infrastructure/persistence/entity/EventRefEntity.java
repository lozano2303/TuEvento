package com.capysoft.commerce.ticket.infrastructure.persistence.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Réplica local de los datos de un evento de Catálogo que necesita Comercio. */
@Entity
@Table(name = "event_ref")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRefEntity {

    @Id
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "name")
    private String name;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
