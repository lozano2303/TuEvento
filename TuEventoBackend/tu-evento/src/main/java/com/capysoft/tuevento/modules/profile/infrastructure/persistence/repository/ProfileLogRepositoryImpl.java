package com.capysoft.tuevento.modules.profile.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.profile.application.mapper.ProfileLogMapper;
import com.capysoft.tuevento.modules.profile.domain.model.ProfileLog;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProfileLogRepositoryImpl implements ProfileLogRepository {

    private final ProfileLogJpaRepository jpaRepository;
    private final ProfileLogMapper mapper;

    @Override
    public ProfileLog save(ProfileLog profileLog) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(profileLog)));
    }

    @Override
    public List<ProfileLog> findByProfileId(Long profileId) {
        return jpaRepository.findByProfileId(profileId).stream().map(mapper::toDomain).toList();
    }
}
