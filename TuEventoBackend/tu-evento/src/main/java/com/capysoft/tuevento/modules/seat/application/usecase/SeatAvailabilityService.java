package com.capysoft.tuevento.modules.seat.application.usecase;

import com.capysoft.tuevento.modules.seat.domain.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Sillas que el mapa muestra como disponibles (estado AVAILABLE), contadas en vivo.
 *
 * El campo available_seats de evento y sección se fija al guardar el layout y no baja con
 * las compras, reservas ni cortesías; las respuestas usan este conteo. Mientras un evento o
 * sección aún no tiene sillas generadas se conserva el valor guardado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeatAvailabilityService {

    private final SeatRepository seatRepository;

    /** Sillas disponibles de cada sección del evento; solo aparecen las secciones con sillas. */
    public Map<Integer, Integer> availableBySection(Integer eventId) {
        return seatRepository.countAvailableBySectionOfEvent(eventId);
    }

    /**
     * Sillas disponibles del evento, o {@code fallback} (el valor guardado) si todavía no tiene
     * sillas o no se pueden contar: los listados se sirven igual, como con el resto de datos.
     */
    public int availableInEvent(Long eventId, int fallback) {
        try {
            Map<Integer, Integer> bySection = availableBySection(Math.toIntExact(eventId));
            if (bySection.isEmpty()) {
                return fallback;
            }
            return bySection.values().stream().mapToInt(Integer::intValue).sum();
        } catch (RuntimeException ex) {
            log.warn("Could not count available seats for event {}: {}", eventId, ex.getMessage());
            return fallback;
        }
    }
}
