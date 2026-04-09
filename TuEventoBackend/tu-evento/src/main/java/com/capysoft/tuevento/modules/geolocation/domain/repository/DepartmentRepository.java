package com.capysoft.tuevento.modules.geolocation.domain.repository;

import java.util.List;
import java.util.Optional;

import com.capysoft.tuevento.modules.geolocation.domain.model.Department;

public interface DepartmentRepository {

    Department save(Department department);
    Optional<Department> findById(Integer departmentId);
    Optional<Department> findByCode(String code);
    List<Department> findAll();
    void deleteById(Integer departmentId);
}
