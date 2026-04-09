package com.capysoft.tuevento.modules.security.domain.repository;

import com.capysoft.tuevento.modules.security.domain.model.UserStatusHistory;

import java.util.List;

public interface UserStatusHistoryRepository {

    UserStatusHistory save(UserStatusHistory userStatusHistory);
    List<UserStatusHistory> findByUserId(Integer userId);
}
