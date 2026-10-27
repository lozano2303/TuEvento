package com.capysoft.identity.security.infrastructure.persistence.repository;

import com.capysoft.identity.security.infrastructure.persistence.entity.OrganizerRoleRevocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizerRoleRevocationJpaRepository extends JpaRepository<OrganizerRoleRevocationEntity, Long> {
}
