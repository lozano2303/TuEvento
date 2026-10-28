package com.capysoft.identity.security.infrastructure.persistence.repository;

import com.capysoft.identity.security.domain.repository.OrganizerRoleRevocationRepository;
import com.capysoft.identity.security.infrastructure.persistence.entity.OrganizerRoleRevocationEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class OrganizerRoleRevocationRepositoryImpl implements OrganizerRoleRevocationRepository {

    private final OrganizerRoleRevocationJpaRepository jpaRepository;

    @Override
    public Long record(Integer userId, Integer revokedBy, String reason) {
        return jpaRepository.save(OrganizerRoleRevocationEntity.builder()
                .userId(userId)
                .revokedBy(revokedBy)
                .reason(reason)
                .createdAt(LocalDateTime.now())
                .build()).getRevocationId();
    }
}
