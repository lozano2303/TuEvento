package com.capysoft.identity.security.domain.repository;

import com.capysoft.identity.security.domain.model.UserStatusHistory;

import java.util.List;

public interface UserStatusHistoryRepository {

    UserStatusHistory save(UserStatusHistory userStatusHistory);
    List<UserStatusHistory> findByUserId(Integer userId);
}
