package com.capysoft.tuevento.modules.event.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "event_layout")
public class EventLayoutEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_layout_id")
    private Long eventLayoutId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "layout_data", nullable = false, columnDefinition = "jsonb")
    private String layoutData;
}
