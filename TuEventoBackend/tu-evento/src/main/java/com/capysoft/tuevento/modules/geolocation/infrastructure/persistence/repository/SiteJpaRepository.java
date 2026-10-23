package com.capysoft.tuevento.modules.geolocation.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.capysoft.tuevento.modules.geolocation.infrastructure.persistence.entity.SiteEntity;

public interface SiteJpaRepository extends JpaRepository<SiteEntity, Integer> {

    List<SiteEntity> findByCity_CityId(Integer cityId);
    List<SiteEntity> findByCity_Department_DepartmentId(Integer departmentId);

    @Query(value = """
            SELECT s.* FROM site s
            WHERE (6371 * ACOS(
                COS(RADIANS(:lat)) * COS(RADIANS(s.latitude)) *
                COS(RADIANS(s.longitude) - RADIANS(:lon)) +
                SIN(RADIANS(:lat)) * SIN(RADIANS(s.latitude))
            )) <= :radiusKm
            """, nativeQuery = true)
    List<SiteEntity> findNearby(
            @Param("lat") BigDecimal latitude,
            @Param("lon") BigDecimal longitude,
            @Param("radiusKm") double radiusKm
    );
}
