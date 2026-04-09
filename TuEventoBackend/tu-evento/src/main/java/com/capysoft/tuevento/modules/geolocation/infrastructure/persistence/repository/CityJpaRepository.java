package com.capysoft.tuevento.modules.geolocation.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.capysoft.tuevento.modules.geolocation.infrastructure.persistence.entity.CityEntity;

public interface CityJpaRepository extends JpaRepository<CityEntity, Integer> {

    Optional<CityEntity> findByCode(String code);
    List<CityEntity> findByDepartment_DepartmentId(Integer departmentId);
}
