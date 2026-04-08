package com.capysoft.tuevento.modules.profile.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.profile.application.mapper.ProfileMapper;
import com.capysoft.tuevento.modules.profile.domain.model.Profile;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProfileRepositoryImpl implements ProfileRepository {

    private final ProfileJpaRepository jpaRepository;
    private final ProfileMapper mapper;

    @Override
    public Profile save(Profile profile) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(profile)));
    }

    @Override
    public Optional<Profile> findById(Long profileId) {
        return jpaRepository.findById(profileId).map(mapper::toDomain);
    }

    @Override
    public Optional<Profile> findByUserId(Integer userId) {
        return jpaRepository.findByUserId(userId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByUserId(Integer userId) {
        return jpaRepository.existsByUserId(userId);
    }

    @Override
    public List<Profile> findAllWithNullStoredFileId() {
        return jpaRepository.findAllByStoredFileIdIsNull()
                .stream().map(mapper::toDomain).toList();
    }
}
