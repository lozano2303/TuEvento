package com.capysoft.tuevento.modules.seat.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seat_block")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatBlockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_block_id")
    private Integer seatBlockId;

    @Column(name = "event_section_id", nullable = false)
    private Integer eventSectionId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;
}
