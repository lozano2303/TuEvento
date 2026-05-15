package com.capysoft.tuevento.modules.category.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "category_event",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"category_id", "event_id"})
    }
)
public class CategoryEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_event_id")
    private Integer categoryEventId;

    /** FK to category — no JPA relation to avoid cross-module coupling. */
    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    /** FK to event — no JPA relation to avoid cross-module coupling. */
    @Column(name = "event_id", nullable = false)
    private Integer eventId;
}
