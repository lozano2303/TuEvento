package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.OrganizerPetitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrganizerPetitionJpaRepository extends JpaRepository<OrganizerPetitionEntity, Integer> {

    @Query("SELECT o FROM OrganizerPetitionEntity o LEFT JOIN FETCH o.storedFile WHERE o.user.userId = :userId AND o.status = 'PENDING'")
    Optional<OrganizerPetitionEntity> findPendingByUserId(@Param("userId") Integer userId);

    @Query("SELECT o FROM OrganizerPetitionEntity o LEFT JOIN FETCH o.storedFile LEFT JOIN FETCH o.user WHERE o.status = :status")
    List<OrganizerPetitionEntity> findByStatus(@Param("status") String status);
}
