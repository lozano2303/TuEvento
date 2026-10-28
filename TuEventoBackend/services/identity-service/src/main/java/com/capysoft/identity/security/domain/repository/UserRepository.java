package com.capysoft.identity.security.domain.repository;

import com.capysoft.identity.security.domain.model.User;

import java.util.Optional;

public interface UserRepository {

    User save(User user);
    Optional<User> findById(Integer userId);
    Optional<User> findByAlias(String alias);
    boolean existsByAlias(String alias);
    void deleteById(Integer userId);
}
