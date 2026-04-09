package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.infrastructure.persistence.entity.UserStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserStatusHistoryJpaRepository extends JpaRepository<UserStatusHistoryEntity, Integer> {

    List<UserStatusHistoryEntity> findByUserUserId(Integer userId);
}
