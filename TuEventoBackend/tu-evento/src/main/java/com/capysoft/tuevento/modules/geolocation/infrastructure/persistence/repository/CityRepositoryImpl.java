package com.capysoft.tuevento.modules.geolocation.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.capysoft.tuevento.modules.geolocation.application.mapper.CityMapper;
import com.capysoft.tuevento.modules.geolocation.domain.model.City;
import com.capysoft.tuevento.modules.geolocation.domain.repository.CityRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CityRepositoryImpl implements CityRepository {

    private final CityJpaRepository jpaRepository;
    private final CityMapper mapper;

    @Override
    public City save(City city) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(city)));
    }

    @Override
    public Optional<City> findById(Integer cityId) {
        return jpaRepository.findById(cityId).map(mapper::toDomain);
    }

    @Override
    public Optional<City> findByCode(String code) {
        return jpaRepository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public List<City> findByDepartmentId(Integer departmentId) {
        return jpaRepository.findByDepartment_DepartmentId(departmentId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<City> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(Integer cityId) {
        jpaRepository.deleteById(cityId);
    }
}
