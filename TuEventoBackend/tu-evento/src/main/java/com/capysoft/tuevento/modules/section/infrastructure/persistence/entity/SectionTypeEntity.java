package com.capysoft.tuevento.modules.section.infrastructure.persistence.entity;

import com.capysoft.tuevento.modules.section.domain.model.SectionCategory;

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

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private SectionCategory category = SectionCategory.GENERAL;
}
