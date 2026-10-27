package com.capysoft.identity.theme.infrastructure.persistence.repository;

import com.capysoft.identity.theme.domain.model.ThemeLog;
import com.capysoft.identity.theme.domain.repository.ThemeLogRepository;
import com.capysoft.identity.theme.infrastructure.persistence.mapper.ThemeInfraMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ThemeLogRepositoryImpl implements ThemeLogRepository {

    private final ThemeLogJpaRepository jpaRepository;
    private final ThemeInfraMapper mapper;

    @Override
    public List<ThemeLog> findByUserThemeId(Integer userThemeId) {
        return jpaRepository.findByUserThemeId(userThemeId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public ThemeLog save(ThemeLog log) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(log)));
    }
}
