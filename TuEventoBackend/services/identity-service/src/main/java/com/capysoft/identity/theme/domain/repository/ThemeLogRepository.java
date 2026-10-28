package com.capysoft.identity.theme.domain.repository;

import com.capysoft.identity.theme.domain.model.ThemeLog;

import java.util.List;

public interface ThemeLogRepository {

    List<ThemeLog> findByUserThemeId(Integer userThemeId);
    ThemeLog save(ThemeLog log);
}
