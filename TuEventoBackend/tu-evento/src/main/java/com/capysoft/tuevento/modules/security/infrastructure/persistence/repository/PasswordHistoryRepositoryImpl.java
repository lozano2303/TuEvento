package com.capysoft.tuevento.modules.security.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.security.application.mapper.PasswordHistoryMapper;
import com.capysoft.tuevento.modules.security.domain.model.PasswordHistory;
import com.capysoft.tuevento.modules.security.domain.repository.PasswordHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PasswordHistoryRepositoryImpl implements PasswordHistoryRepository {

    private final PasswordHistoryJpaRepository jpaRepository;
    private final PasswordHistoryMapper mapper;

    @Override
    public PasswordHistory save(PasswordHistory passwordHistory) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(passwordHistory)));
    }

    @Override
    public List<PasswordHistory> findRecentByUserId(Integer userId, int limit) {
        return jpaRepository.findRecentByUserId(userId, PageRequest.of(0, limit))
                .stream().map(mapper::toDomain).toList();
    }
}
