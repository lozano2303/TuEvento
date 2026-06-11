package com.capysoft.tuevento.modules.seat.infrastructure.persistence.entity;

import com.capysoft.tuevento.modules.seat.domain.model.SeatStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "seat_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_log_id")
    private Integer seatLogId;

    @Column(name = "seat_id", nullable = false)
    private Integer seatId;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 20)
    private SeatStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private SeatStatus newStatus;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "changed_by")
    private Integer changedBy;

    @Column(name = "reason", length = 255)
    private String reason;
}
