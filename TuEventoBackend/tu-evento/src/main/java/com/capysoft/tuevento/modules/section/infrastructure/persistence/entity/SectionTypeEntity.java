package com.capysoft.tuevento.modules.section.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "section_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "section_type_id")
    private Integer sectionTypeId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;
}
