package com.capysoft.tuevento.modules.seat.application.usecase;

import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeatAvailabilityService — sillas disponibles en vivo")
class SeatAvailabilityServiceTest {

    @Mock SeatRepository seatRepository;
    @InjectMocks SeatAvailabilityService service;

    @Test
    @DisplayName("devuelve las disponibles de cada sección tal cual las cuenta el repositorio")
    void bySection() {
        when(seatRepository.countAvailableBySectionOfEvent(5)).thenReturn(Map.of(10, 3, 11, 0));

        assertThat(service.availableBySection(5)).containsOnly(Map.entry(10, 3), Map.entry(11, 0));
    }

    @Test
    @DisplayName("el evento suma las disponibles de todas sus secciones, aunque alguna esté agotada")
    void eventSumsSections() {
        when(seatRepository.countAvailableBySectionOfEvent(5)).thenReturn(Map.of(10, 3, 11, 0, 12, 40));

        assertThat(service.availableInEvent(5L, 500)).isEqualTo(43);
    }

    @Test
    @DisplayName("un evento sin sillas generadas conserva el valor guardado")
    void eventWithoutSeatsKeepsStoredValue() {
        when(seatRepository.countAvailableBySectionOfEvent(5)).thenReturn(Map.of());

        assertThat(service.availableInEvent(5L, 120)).isEqualTo(120);
    }

    @Test
    @DisplayName("si el conteo falla, devuelve el valor guardado")
    void failureKeepsStoredValue() {
        when(seatRepository.countAvailableBySectionOfEvent(5)).thenThrow(new IllegalStateException("db down"));

        assertThat(service.availableInEvent(5L, 120)).isEqualTo(120);
    }
}
