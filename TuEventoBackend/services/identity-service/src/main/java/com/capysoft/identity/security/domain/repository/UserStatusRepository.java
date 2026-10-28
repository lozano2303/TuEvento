package com.capysoft.identity.security.domain.repository;

import com.capysoft.identity.security.domain.model.UserStatus;

import java.util.List;
import java.util.Optional;

public interface UserStatusRepository {

    Optional<UserStatus> findById(Integer userStatusId);
    Optional<UserStatus> findByCode(String code);
    List<UserStatus> findAll();
}
