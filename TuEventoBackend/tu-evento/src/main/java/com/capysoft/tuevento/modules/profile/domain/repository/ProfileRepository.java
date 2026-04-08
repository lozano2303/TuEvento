package com.capysoft.tuevento.modules.profile.domain.repository;

import com.capysoft.tuevento.modules.profile.domain.model.Profile;

import java.util.Optional;

public interface ProfileRepository {

    Profile save(Profile profile);
    Optional<Profile> findById(Long profileId);
    Optional<Profile> findByUserId(Integer userId);
    boolean existsByUserId(Integer userId);
}
