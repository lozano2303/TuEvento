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
    public Optional<CategoryEvent> findById(Integer id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<CategoryEvent> findByEventId(Integer eventId) {
        return mapper.toDomainList(jpaRepository.findByEventId(eventId));
    }

    @Override
    public List<CategoryEvent> findByCategoryId(Integer categoryId) {
        return mapper.toDomainList(jpaRepository.findByCategoryId(categoryId));
    }

    @Override
    public boolean existsByCategoryIdAndEventId(Integer categoryId, Integer eventId) {
        return jpaRepository.existsByCategoryIdAndEventId(categoryId, eventId);
    }

    @Override
    public void deleteById(Integer id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void deleteByEventId(Integer eventId) {
        jpaRepository.deleteByEventId(eventId);
    }
}
