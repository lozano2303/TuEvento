package com.capysoft.tuevento.modules.seat.infrastructure.persistence.entity;

import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import com.capysoft.tuevento.modules.seat.domain.model.SeatType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_id")
    private Integer seatId;

    @Column(name = "seat_block_id", nullable = false)
    private Integer seatBlockId;

    @Column(name = "event_section_id", nullable = false)
    private Integer eventSectionId;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "row", nullable = false)
    private Integer row;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private SeatType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SeatStatus status;

    @Column(name = "reserved_by")
    private Integer reservedBy;

    @Column(name = "reserved_until")
    private java.time.LocalDateTime reservedUntil;
}
