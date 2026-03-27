package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.application.mapper.UserStatusHistoryMapper;
import com.capysoft.tuevento.modules.security.domain.model.UserStatusHistory;
import com.capysoft.tuevento.modules.security.domain.repository.UserStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserStatusHistoryRepositoryImpl implements UserStatusHistoryRepository {

    private final UserStatusHistoryJpaRepository jpaRepository;
    private final UserStatusHistoryMapper mapper;

    @Override
    public UserStatusHistory save(UserStatusHistory userStatusHistory) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(userStatusHistory)));
    }

    @Override
    public List<UserStatusHistory> findByUserId(Integer userId) {
        return jpaRepository.findByUserUserId(userId).stream().map(mapper::toDomain).toList();
    }
}
