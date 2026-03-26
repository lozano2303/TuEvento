package com.capysoft.tuevento.modules.security.domain.repository;

import com.capysoft.tuevento.modules.security.domain.model.PasswordHistory;

import java.util.List;

public interface PasswordHistoryRepository {

    PasswordHistory save(PasswordHistory passwordHistory);
    List<PasswordHistory> findRecentByUserId(Integer userId, int limit);
}
