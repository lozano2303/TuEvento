package com.capysoft.tuevento.modules.theme.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.theme.domain.model.ThemeCustomization;
import com.capysoft.tuevento.modules.theme.domain.repository.ThemeCustomizationRepository;
import com.capysoft.tuevento.modules.theme.infrastructure.persistence.mapper.ThemeInfraMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ThemeCustomizationRepositoryImpl implements ThemeCustomizationRepository {

    private final ThemeCustomizationJpaRepository jpaRepository;
    private final ThemeInfraMapper mapper;

    @Override
    public List<ThemeCustomization> findByUserThemeId(Integer userThemeId) {
        return jpaRepository.findByUserThemeId(userThemeId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<ThemeCustomization> findByUserThemeIdAndProperty(Integer userThemeId, String property) {
        return jpaRepository.findByUserThemeIdAndProperty(userThemeId, property).map(mapper::toDomain);
    }

    @Override
    public ThemeCustomization save(ThemeCustomization customization) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(customization)));
    }

    @Override
    @Transactional
    public void deleteByUserThemeIdAndProperty(Integer userThemeId, String property) {
        jpaRepository.deleteByUserThemeIdAndProperty(userThemeId, property);
    }
}
