package com.capysoft.tuevento.modules.event.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "event_media")
public class EventMediaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "media_id")
    private Long mediaId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "img_url", nullable = false, length = 255)
    private String imgUrl;
}
