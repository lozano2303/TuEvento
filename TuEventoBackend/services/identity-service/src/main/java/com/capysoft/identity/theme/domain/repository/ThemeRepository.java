package com.capysoft.identity.theme.domain.repository;

import com.capysoft.identity.theme.domain.model.Theme;

import java.util.List;
import java.util.Optional;

public interface ThemeRepository {

    List<Theme> findAll();
    Optional<Theme> findById(Integer id);
    Optional<Theme> findByName(String name);
}
