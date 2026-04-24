package com.capysoft.tuevento.modules.theme.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.theme.domain.model.Theme;
import com.capysoft.tuevento.modules.theme.domain.repository.ThemeRepository;
import com.capysoft.tuevento.modules.theme.infrastructure.persistence.mapper.ThemeInfraMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ThemeRepositoryImpl implements ThemeRepository {

    private final ThemeJpaRepository jpaRepository;
    private final ThemeInfraMapper mapper;

    @Override
    public List<Theme> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Theme> findById(Integer id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Theme> findByName(String name) {
        return jpaRepository.findByName(name).map(mapper::toDomain);
    }
}
