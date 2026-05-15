package com.capysoft.tuevento.modules.category.infrastructure.persistence.entity;

import com.capysoft.tuevento.shared.infrastructure.persistence.JpaAuditingEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "category")
public class CategoryEntity extends JpaAuditingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "description", nullable = false, length = 100)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "visible", nullable = false)
    private boolean visible;

    /** Nullable — null means this is a root category. No JPA relation to avoid self-join complexity. */
    @Column(name = "dad_id")
    private Long dadId;
}
