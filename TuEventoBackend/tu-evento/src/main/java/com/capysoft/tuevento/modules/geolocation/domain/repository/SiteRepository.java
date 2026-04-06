package com.capysoft.tuevento.modules.geolocation.domain.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.capysoft.tuevento.modules.geolocation.domain.model.Site;

public interface SiteRepository {

    Site save(Site site);
    Optional<Site> findById(Integer siteId);
    List<Site> findByCityId(Integer cityId);
    List<Site> findByDepartmentId(Integer departmentId);
    List<Site> findNearby(BigDecimal latitude, BigDecimal longitude, double radiusKm);
    List<Site> findAll();
    void deleteById(Integer siteId);
}
