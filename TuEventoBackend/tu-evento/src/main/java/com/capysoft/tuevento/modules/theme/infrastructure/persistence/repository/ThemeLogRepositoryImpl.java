package com.capysoft.tuevento.modules.theme.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.theme.domain.model.ThemeLog;
import com.capysoft.tuevento.modules.theme.domain.repository.ThemeLogRepository;
import com.capysoft.tuevento.modules.theme.infrastructure.persistence.mapper.ThemeInfraMapper;
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
