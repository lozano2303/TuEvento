package com.capysoft.tuevento.modules.geolocation.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.capysoft.tuevento.modules.geolocation.infrastructure.persistence.entity.DepartmentEntity;

public interface DepartmentJpaRepository extends JpaRepository<DepartmentEntity, Integer> {

    Optional<DepartmentEntity> findByCode(String code);
}
