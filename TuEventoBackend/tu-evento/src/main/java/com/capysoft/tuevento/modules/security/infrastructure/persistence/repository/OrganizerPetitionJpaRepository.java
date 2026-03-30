package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.OrganizerPetitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrganizerPetitionJpaRepository extends JpaRepository<OrganizerPetitionEntity, Integer> {

    @Query("SELECT o FROM OrganizerPetitionEntity o WHERE o.user.userId = :userId AND o.status = 'PENDING'")
    Optional<OrganizerPetitionEntity> findPendingByUserId(@Param("userId") Integer userId);

    List<OrganizerPetitionEntity> findByStatus(String status);
}
