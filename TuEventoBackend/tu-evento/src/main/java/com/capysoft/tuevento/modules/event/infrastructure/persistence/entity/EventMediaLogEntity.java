package com.capysoft.tuevento.modules.event.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "event_media_log")
public class EventMediaLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "media_log_id")
    private Long mediaLogId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "media_id", nullable = false)
    private Long mediaId;

    @Column(name = "img_url", nullable = false, length = 255)
    private String imgUrl;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "is_visible", nullable = false)
    private boolean isVisible;
}
