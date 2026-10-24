package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.OrganizerRoleRevocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizerRoleRevocationJpaRepository extends JpaRepository<OrganizerRoleRevocationEntity, Long> {
}
