package com.capysoft.tuevento.modules.geolocation.domain.repository;

import java.util.List;
import java.util.Optional;

import com.capysoft.tuevento.modules.geolocation.domain.model.City;

public interface CityRepository {

    City save(City city);
    Optional<City> findById(Integer cityId);
    Optional<City> findByCode(String code);
    List<City> findByDepartmentId(Integer departmentId);
    List<City> findAll();
    void deleteById(Integer cityId);
}
