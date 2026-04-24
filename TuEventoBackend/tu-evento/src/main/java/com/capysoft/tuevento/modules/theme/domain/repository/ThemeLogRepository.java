package com.capysoft.tuevento.modules.theme.domain.repository;

import com.capysoft.tuevento.modules.theme.domain.model.ThemeLog;

import java.util.List;

public interface ThemeLogRepository {

    List<ThemeLog> findByUserThemeId(Integer userThemeId);
    ThemeLog save(ThemeLog log);
}
