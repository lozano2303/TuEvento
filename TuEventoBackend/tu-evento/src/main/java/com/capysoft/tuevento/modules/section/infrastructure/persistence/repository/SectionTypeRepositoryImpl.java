package com.capysoft.tuevento.modules.section.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.section.domain.model.SectionType;
import com.capysoft.tuevento.modules.section.domain.repository.SectionTypeRepository;
import com.capysoft.tuevento.modules.section.infrastructure.persistence.entity.SectionTypeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SectionTypeRepositoryImpl implements SectionTypeRepository {

    private final SectionTypeJpaRepository jpaRepository;

    @Override
    public List<SectionType> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<SectionType> findById(Integer sectionTypeId) {
        return jpaRepository.findById(sectionTypeId).map(this::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return jpaRepository.existsByName(name);
    }

    @Override
    public SectionType save(SectionType sectionType) {
        SectionTypeEntity entity = SectionTypeEntity.builder()
                .sectionTypeId(sectionType.getSectionTypeId())
                .name(sectionType.getName())
                .build();
        return toDomain(jpaRepository.save(entity));
    }

    private SectionType toDomain(SectionTypeEntity entity) {
        return SectionType.builder()
                .sectionTypeId(entity.getSectionTypeId())
                .name(entity.getName())
                .build();
    }
}
