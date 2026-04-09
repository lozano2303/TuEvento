package com.capysoft.tuevento.modules.security.domain.repository;

import com.capysoft.tuevento.modules.security.domain.model.OrganizerPetition;

import java.util.List;
import java.util.Optional;

public interface OrganizerPetitionRepository {

    OrganizerPetition save(OrganizerPetition organizerPetition);
    Optional<OrganizerPetition> findById(Integer organizerPetitionId);
    Optional<OrganizerPetition> findPendingByUserId(Integer userId);
    List<OrganizerPetition> findByStatus(String status);
}
