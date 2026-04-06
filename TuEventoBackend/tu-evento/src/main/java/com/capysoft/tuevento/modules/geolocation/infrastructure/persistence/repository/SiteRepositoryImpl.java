package com.capysoft.tuevento.modules.geolocation.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.capysoft.tuevento.modules.geolocation.application.mapper.SiteMapper;
import com.capysoft.tuevento.modules.geolocation.domain.model.Site;
import com.capysoft.tuevento.modules.geolocation.domain.repository.SiteRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SiteRepositoryImpl implements SiteRepository {

    private final SiteJpaRepository jpaRepository;
    private final SiteMapper mapper;

    @Override
    public Site save(Site site) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(site)));
    }

    @Override
    public Optional<Site> findById(Integer siteId) {
        return jpaRepository.findById(siteId).map(mapper::toDomain);
    }

    @Override
    public List<Site> findByCityId(Integer cityId) {
        return jpaRepository.findByCity_CityId(cityId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Site> findByDepartmentId(Integer departmentId) {
        return jpaRepository.findByCity_Department_DepartmentId(departmentId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Site> findNearby(BigDecimal latitude, BigDecimal longitude, double radiusKm) {
        return jpaRepository.findNearby(latitude, longitude, radiusKm).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Site> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(Integer siteId) {
        jpaRepository.deleteById(siteId);
    }
}
