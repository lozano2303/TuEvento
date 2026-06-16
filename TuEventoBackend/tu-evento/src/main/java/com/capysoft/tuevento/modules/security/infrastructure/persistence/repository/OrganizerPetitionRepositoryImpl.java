package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.application.mapper.OrganizerPetitionMapper;
import com.capysoft.tuevento.modules.security.domain.model.OrganizerPetition;
import com.capysoft.tuevento.modules.security.domain.repository.OrganizerPetitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrganizerPetitionRepositoryImpl implements OrganizerPetitionRepository {

    private final OrganizerPetitionJpaRepository jpaRepository;
    private final OrganizerPetitionMapper mapper;

    @Override
    public OrganizerPetition save(OrganizerPetition organizerPetition) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(organizerPetition)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrganizerPetition> findById(Integer organizerPetitionId) {
        return jpaRepository.findById(organizerPetitionId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrganizerPetition> findPendingByUserId(Integer userId) {
        return jpaRepository.findPendingByUserId(userId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizerPetition> findByStatus(String status) {
        return jpaRepository.findByStatus(status).stream().map(mapper::toDomain).toList();
    }
}
