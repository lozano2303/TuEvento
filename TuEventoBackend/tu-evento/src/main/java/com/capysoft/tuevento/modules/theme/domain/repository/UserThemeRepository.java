package com.capysoft.tuevento.modules.theme.domain.repository;

import com.capysoft.tuevento.modules.theme.domain.model.UserTheme;

import java.util.List;
import java.util.Optional;

public interface UserThemeRepository {

    List<UserTheme> findByUserId(Integer userId);
    Optional<UserTheme> findActiveByUserId(Integer userId);
    Optional<UserTheme> findById(Integer id);
    boolean existsByUserIdAndThemeId(Integer userId, Integer themeId);
    UserTheme save(UserTheme userTheme);

    /** Sets isActive = false for all UserTheme records belonging to the given user. */
    void deactivateAllByUserId(Integer userId);
}
