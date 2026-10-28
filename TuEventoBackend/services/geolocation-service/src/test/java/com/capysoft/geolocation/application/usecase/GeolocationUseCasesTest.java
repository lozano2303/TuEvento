package com.capysoft.geolocation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import com.capysoft.common.domain.exception.NotFoundException;
import com.capysoft.geolocation.application.dto.request.CreateSiteRequest;
import com.capysoft.geolocation.application.dto.request.UpdateSiteRequest;
import com.capysoft.geolocation.application.dto.response.SiteResponse;
import com.capysoft.geolocation.domain.event.SiteCreatedEvent;
import com.capysoft.geolocation.domain.event.SiteUpdatedEvent;
import com.capysoft.geolocation.domain.exception.SiteAlreadyExistsException;
import com.capysoft.geolocation.domain.model.City;
import com.capysoft.geolocation.domain.model.Department;
import com.capysoft.geolocation.domain.model.Site;
import com.capysoft.geolocation.domain.repository.CityRepository;
import com.capysoft.geolocation.domain.repository.DepartmentRepository;
import com.capysoft.geolocation.domain.repository.SiteRepository;

/**
 * Casos de uso de geolocalización (A6 del plan: el módulo no tenía tests de caracterización).
 */
@DisplayName("Casos de uso de geolocalización")
class GeolocationUseCasesTest {

    private static final Department ANTIOQUIA = Department.builder().departmentId(5).name("Antioquia").code("05").build();
    private static final City MEDELLIN = City.builder().cityId(1).department(ANTIOQUIA).name("Medellín").code("05001").build();

    private final SiteRepository sites = mock(SiteRepository.class);
    private final CityRepository cities = mock(CityRepository.class);
    private final DepartmentRepository departments = mock(DepartmentRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);

    private static Site coliseo() {
        return Site.builder().siteId(9).city(MEDELLIN).name("Coliseo").address("Calle 1").capacity(500)
                .latitude(new BigDecimal("6.25")).longitude(new BigDecimal("-75.56")).build();
    }

    private static CreateSiteRequest createRequest() {
        CreateSiteRequest request = new CreateSiteRequest();
        request.setCityId(1);
        request.setName("Coliseo");
        request.setAddress("Calle 1");
        request.setCapacity(500);
        request.setLatitude(new BigDecimal("6.25"));
        request.setLongitude(new BigDecimal("-75.56"));
        return request;
    }

    @Nested
    @DisplayName("crear sede")
    class Create {

        private final CreateSiteUseCase useCase = new CreateSiteUseCase(sites, cities, events);

        @Test
        @DisplayName("guarda la sede con su ciudad y publica SiteCreated")
        void createsSite() {
            when(cities.findById(1)).thenReturn(Optional.of(MEDELLIN));
            when(sites.findNearby(any(), any(), anyDouble())).thenReturn(List.of());
            when(sites.save(any())).thenAnswer(inv -> {
                Site s = inv.getArgument(0);
                s.setSiteId(9);
                return s;
            });

            SiteResponse response = useCase.createSite(createRequest());

            assertThat(response.getSiteId()).isEqualTo(9);
            assertThat(response.getCityName()).isEqualTo("Medellín");
            assertThat(response.getDepartmentName()).isEqualTo("Antioquia");
            verify(events).publishEvent(any(SiteCreatedEvent.class));
        }

        @Test
        @DisplayName("una sede a menos de 50 m de otra se rechaza como duplicada")
        void rejectsDuplicateNearby() {
            when(cities.findById(1)).thenReturn(Optional.of(MEDELLIN));
            when(sites.findNearby(any(), any(), eq(0.05))).thenReturn(List.of(coliseo()));

            assertThatThrownBy(() -> useCase.createSite(createRequest())).isInstanceOf(SiteAlreadyExistsException.class);
            verify(sites, never()).save(any());
        }

        @Test
        @DisplayName("una ciudad inexistente da CITY_NOT_FOUND")
        void unknownCity() {
            when(cities.findById(1)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.createSite(createRequest()))
                    .isInstanceOf(NotFoundException.class).hasFieldOrPropertyWithValue("code", "CITY_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("editar sede")
    class Update {

        private final UpdateSiteUseCase useCase = new UpdateSiteUseCase(sites, events);

        @Test
        @DisplayName("solo cambia los campos enviados y publica SiteUpdated")
        void updatesOnlyGivenFields() {
            when(sites.findById(9)).thenReturn(Optional.of(coliseo()));
            when(sites.save(any())).thenAnswer(inv -> inv.getArgument(0));
            UpdateSiteRequest request = new UpdateSiteRequest();
            request.setCapacity(800);
            request.setLatitude(new BigDecimal("6.25"));
            request.setLongitude(new BigDecimal("-75.56"));

            SiteResponse response = useCase.updateSite(9, request);

            assertThat(response.getCapacity()).isEqualTo(800);
            assertThat(response.getName()).isEqualTo("Coliseo");
            ArgumentCaptor<SiteUpdatedEvent> event = ArgumentCaptor.forClass(SiteUpdatedEvent.class);
            verify(events).publishEvent(event.capture());
            assertThat(event.getValue().getSiteId()).isEqualTo(9);
        }

        @Test
        @DisplayName("una sede inexistente da SITE_NOT_FOUND")
        void unknownSite() {
            when(sites.findById(9)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCase.updateSite(9, new UpdateSiteRequest()))
                    .isInstanceOf(NotFoundException.class).hasFieldOrPropertyWithValue("code", "SITE_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("consultas")
    class Queries {

        @Test
        @DisplayName("una sede por id, con ciudad y departamento; inexistente da 404")
        void getSite() {
            when(sites.findById(9)).thenReturn(Optional.of(coliseo()));
            when(sites.findById(10)).thenReturn(Optional.empty());
            GetSiteUseCase useCase = new GetSiteUseCase(sites);

            assertThat(useCase.getSite(9).getDepartmentName()).isEqualTo("Antioquia");
            assertThatThrownBy(() -> useCase.getSite(10)).isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("una ciudad por id, con su departamento; inexistente da CITY_NOT_FOUND")
        void getCity() {
            when(cities.findById(1)).thenReturn(Optional.of(MEDELLIN));
            when(cities.findById(2)).thenReturn(Optional.empty());
            GetCityUseCase useCase = new GetCityUseCase(cities);

            assertThat(useCase.getCity(1).getDepartmentName()).isEqualTo("Antioquia");
            assertThatThrownBy(() -> useCase.getCity(2))
                    .isInstanceOf(NotFoundException.class).hasFieldOrPropertyWithValue("code", "CITY_NOT_FOUND");
        }

        @Test
        @DisplayName("sedes cercanas, por ciudad y por departamento")
        void siteLists() {
            when(sites.findNearby(new BigDecimal("6.2"), new BigDecimal("-75.5"), 10.0)).thenReturn(List.of(coliseo()));
            when(sites.findByCityId(1)).thenReturn(List.of(coliseo()));
            when(sites.findByDepartmentId(5)).thenReturn(List.of(coliseo()));

            assertThat(new GetNearbySitesUseCase(sites).getNearbySites(new BigDecimal("6.2"), new BigDecimal("-75.5"), 10.0))
                    .extracting(SiteResponse::getName).containsExactly("Coliseo");
            assertThat(new GetSitesByCityUseCase(sites).getSitesByCity(1)).hasSize(1);
            assertThat(new GetSitesByDepartmentUseCase(sites).getSitesByDepartment(5)).hasSize(1);
        }

        @Test
        @DisplayName("departamentos y sus ciudades; un departamento inexistente da 404")
        void departmentsAndCities() {
            when(departments.findAll()).thenReturn(List.of(ANTIOQUIA));
            when(departments.findById(5)).thenReturn(Optional.of(ANTIOQUIA));
            when(departments.findById(99)).thenReturn(Optional.empty());
            when(cities.findByDepartmentId(5)).thenReturn(List.of(MEDELLIN));
            GetCitiesByDepartmentUseCase citiesUseCase = new GetCitiesByDepartmentUseCase(departments, cities);

            assertThat(new GetDepartmentsUseCase(departments).getDepartments()).hasSize(1);
            assertThat(citiesUseCase.getCitiesByDepartment(5)).singleElement()
                    .satisfies(c -> assertThat(c.getName()).isEqualTo("Medellín"));
            assertThatThrownBy(() -> citiesUseCase.getCitiesByDepartment(99))
                    .isInstanceOf(NotFoundException.class).hasFieldOrPropertyWithValue("code", "DEPARTMENT_NOT_FOUND");
        }
    }
}
