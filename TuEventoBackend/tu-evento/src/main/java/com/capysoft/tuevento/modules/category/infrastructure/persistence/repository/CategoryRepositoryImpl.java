package com.capysoft.tuevento.modules.category.infrastructure.persistence.repository;

import com.capysoft.tuevento.modules.category.domain.model.Category;
import com.capysoft.tuevento.modules.category.domain.repository.CategoryRepository;
import com.capysoft.tuevento.modules.category.infrastructure.persistence.mapper.CategoryEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepository {

    private final CategoryJpaRepository  jpaRepository;
    private final CategoryEntityMapper   mapper;

    @Override
    public Category save(Category category) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(category)));
    }

    @Override
    public Optional<Category> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Category> findAll() {
        return mapper.toDomainList(jpaRepository.findAll());
    }

    @Override
    public List<Category> findAllActive() {
        return mapper.toDomainList(jpaRepository.findByActiveTrue());
    }

    @Override
    public List<Category> findByDadId(Long dadId) {
        return mapper.toDomainList(jpaRepository.findByDadId(dadId));
    }

    @Override
    public List<Category> findRootCategories() {
        return mapper.toDomainList(jpaRepository.findByDadIdIsNull());
    }

    @Override
    public boolean existsByName(String name) {
        return jpaRepository.existsByName(name);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
