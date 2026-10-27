package com.capysoft.tuevento.modules.geolocation.application.usecase;

import com.capysoft.tuevento.modules.geolocation.application.dto.response.CityResponse;
import com.capysoft.tuevento.modules.geolocation.domain.model.City;
import com.capysoft.tuevento.modules.geolocation.domain.model.Department;
import com.capysoft.tuevento.modules.geolocation.domain.repository.CityRepository;
import com.capysoft.tuevento.shared.domain.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GetCityUseCase")
class GetCityUseCaseTest {

    private final CityRepository cities = mock(CityRepository.class);
    private final GetCityUseCase useCase = new GetCityUseCase(cities);

    @Test
    @DisplayName("devuelve la ciudad con su departamento")
    void returnsCity() {
        Department antioquia = Department.builder().departmentId(5).name("Antioquia").code("05").build();
        when(cities.findById(1)).thenReturn(Optional.of(
                City.builder().cityId(1).department(antioquia).name("Medellín").code("05001").build()));

        CityResponse city = useCase.getCity(1);

        assertThat(city.getName()).isEqualTo("Medellín");
        assertThat(city.getDepartmentId()).isEqualTo(5);
        assertThat(city.getDepartmentName()).isEqualTo("Antioquia");
        assertThat(city.getCode()).isEqualTo("05001");
    }

    @Test
    @DisplayName("una ciudad inexistente da CITY_NOT_FOUND")
    void notFound() {
        when(cities.findById(2)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getCity(2))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("code", "CITY_NOT_FOUND");
    }
}
