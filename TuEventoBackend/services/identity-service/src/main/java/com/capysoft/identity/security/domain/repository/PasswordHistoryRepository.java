package com.capysoft.identity.security.domain.repository;

import com.capysoft.identity.security.domain.model.PasswordHistory;

import java.util.List;

public interface PasswordHistoryRepository {

    PasswordHistory save(PasswordHistory passwordHistory);
    List<PasswordHistory> findRecentByUserId(Integer userId, int limit);
}
