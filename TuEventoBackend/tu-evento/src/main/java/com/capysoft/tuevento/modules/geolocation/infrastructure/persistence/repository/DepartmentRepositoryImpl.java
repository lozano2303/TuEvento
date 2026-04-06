package com.capysoft.tuevento.modules.geolocation.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.capysoft.tuevento.modules.geolocation.application.mapper.DepartmentMapper;
import com.capysoft.tuevento.modules.geolocation.domain.model.Department;
import com.capysoft.tuevento.modules.geolocation.domain.repository.DepartmentRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class DepartmentRepositoryImpl implements DepartmentRepository {

    private final DepartmentJpaRepository jpaRepository;
    private final DepartmentMapper mapper;

    @Override
    public Department save(Department department) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(department)));
    }

    @Override
    public Optional<Department> findById(Integer departmentId) {
        return jpaRepository.findById(departmentId).map(mapper::toDomain);
    }

    @Override
    public Optional<Department> findByCode(String code) {
        return jpaRepository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public List<Department> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(Integer departmentId) {
        jpaRepository.deleteById(departmentId);
    }
}
