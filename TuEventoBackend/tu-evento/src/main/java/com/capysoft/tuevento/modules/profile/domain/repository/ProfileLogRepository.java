package com.capysoft.tuevento.modules.profile.domain.repository;

import com.capysoft.tuevento.modules.profile.domain.model.ProfileLog;

import java.util.List;

public interface ProfileLogRepository {

    ProfileLog save(ProfileLog profileLog);
    List<ProfileLog> findByProfileId(Long profileId);
}
