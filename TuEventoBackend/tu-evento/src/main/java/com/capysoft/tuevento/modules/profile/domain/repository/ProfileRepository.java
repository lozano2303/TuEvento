package com.capysoft.tuevento.modules.profile.domain.repository;

import java.util.List;
import java.util.Optional;

import com.capysoft.tuevento.modules.profile.domain.model.Profile;

public interface ProfileRepository {

    Profile save(Profile profile);
    Optional<Profile> findById(Long profileId);
    Optional<Profile> findByUserId(Integer userId);
    boolean existsByUserId(Integer userId);
    List<Profile> findAllWithNullStoredFileId();
}
