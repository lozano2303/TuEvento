package com.capysoft.tuevento.modules.category.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.category.domain.model.CategoryEvent;
import com.capysoft.tuevento.modules.category.domain.repository.CategoryEventRepository;
import com.capysoft.tuevento.modules.category.infrastructure.persistence.mapper.CategoryEventEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CategoryEventRepositoryImpl implements CategoryEventRepository {

    private final CategoryEventJpaRepository jpaRepository;
    private final CategoryEventEntityMapper  mapper;

    @Override
    public CategoryEvent save(CategoryEvent categoryEvent) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(categoryEvent)));
    }

    @Override
    public Optional<CategoryEvent> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<CategoryEvent> findByEventId(Long eventId) {
        return mapper.toDomainList(jpaRepository.findByEventId(eventId));
    }

    @Override
    public List<CategoryEvent> findByCategoryId(Long categoryId) {
        return mapper.toDomainList(jpaRepository.findByCategoryId(categoryId));
    }

    @Override
    public boolean existsByCategoryIdAndEventId(Long categoryId, Long eventId) {
        return jpaRepository.existsByCategoryIdAndEventId(categoryId, eventId);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void deleteByEventId(Long eventId) {
        jpaRepository.deleteByEventId(eventId);
    }
}
