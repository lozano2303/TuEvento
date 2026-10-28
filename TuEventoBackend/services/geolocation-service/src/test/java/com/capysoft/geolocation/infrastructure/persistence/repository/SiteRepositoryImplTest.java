package com.capysoft.geolocation.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.capysoft.geolocation.application.mapper.CityMapperImpl;
import com.capysoft.geolocation.application.mapper.DepartmentMapperImpl;
import com.capysoft.geolocation.application.mapper.SiteMapperImpl;
import com.capysoft.geolocation.domain.model.Site;
import com.capysoft.geolocation.infrastructure.persistence.entity.CityEntity;
import com.capysoft.geolocation.infrastructure.persistence.entity.DepartmentEntity;
import com.capysoft.geolocation.infrastructure.persistence.entity.SiteEntity;

/** Consultas de sedes contra una base real (H2): por departamento y búsqueda por distancia. */
@DataJpaTest
// En producción las tablas las migra el monolito (validate); aquí H2 las crea desde las entidades
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({SiteRepositoryImpl.class, SiteMapperImpl.class, CityMapperImpl.class, DepartmentMapperImpl.class})
@DisplayName("SiteRepositoryImpl")
class SiteRepositoryImplTest {

    @Autowired SiteRepositoryImpl repository;
    @Autowired SiteJpaRepository siteJpa;
    @Autowired CityJpaRepository cityJpa;
    @Autowired DepartmentJpaRepository departmentJpa;

    private CityEntity medellin;

    @BeforeEach
    void data() {
        DepartmentEntity antioquia = departmentJpa.save(DepartmentEntity.builder().name("Antioquia").code("05").build());
        DepartmentEntity cundinamarca = departmentJpa.save(DepartmentEntity.builder().name("Cundinamarca").code("25").build());
        medellin = cityJpa.save(CityEntity.builder().department(antioquia).name("Medellín").code("05001").build());
        CityEntity bogota = cityJpa.save(CityEntity.builder().department(cundinamarca).name("Bogotá").code("11001").build());
        site(medellin, "Atanasio Girardot", "6.256900", "-75.590200");
        site(bogota, "Movistar Arena", "4.648500", "-74.077600");
    }

    private void site(CityEntity city, String name, String lat, String lon) {
        siteJpa.save(SiteEntity.builder().city(city).name(name).address("Dirección").capacity(1000)
                .latitude(new BigDecimal(lat)).longitude(new BigDecimal(lon)).build());
    }

    @Test
    @DisplayName("lista las sedes de un departamento")
    void findsByDepartment() {
        Integer antioquia = medellin.getDepartment().getDepartmentId();

        assertThat(repository.findByDepartmentId(antioquia)).extracting(Site::getName).containsExactly("Atanasio Girardot");
    }

    @Test
    @DisplayName("encuentra las sedes dentro del radio y excluye las lejanas")
    void findsNearbyWithinRadius() {
        assertThat(repository.findNearby(new BigDecimal("6.25"), new BigDecimal("-75.58"), 5))
                .extracting(Site::getName).containsExactly("Atanasio Girardot");
        assertThat(repository.findNearby(new BigDecimal("6.25"), new BigDecimal("-75.58"), 0.5)).isEmpty();
    }

    @Test
    @DisplayName("una sede con exactamente las mismas coordenadas cuenta como cercana (sin error de ACOS)")
    void findsSiteAtExactSameCoordinates() {
        assertThat(repository.findNearby(new BigDecimal("6.256900"), new BigDecimal("-75.590200"), 0.05))
                .extracting(Site::getName).containsExactly("Atanasio Girardot");
    }
}
