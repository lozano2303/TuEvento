package com.capysoft.tuevento.modules.seat.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.capysoft.tuevento.modules.section.domain.model.EventSection;
import com.capysoft.tuevento.modules.section.domain.repository.EventSectionRepository;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.capysoft.tuevento.shared.infrastructure.config.DataInitializer;

/**
 * Regresión de la doble reserva (F2): dos peticiones simultáneas sobre la misma silla contra
 * una base real. Con el lock pesimista, la segunda espera al commit de la primera, ve la silla
 * RESERVED y falla.
 */
@ActiveProfiles("test")
@SpringBootTest
@DisplayName("SeatReservationService — dos reservas simultáneas")
class SeatReservationConcurrencyTest {

    private static final int SEAT_ID = 7001;

    // H2 arranca sin Liquibase: se omite la creación del admin
    @MockitoBean DataInitializer dataInitializer;
    @MockitoBean EventSectionRepository eventSectionRepository;

    @Autowired SeatReservationService service;
    @Autowired JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM seat WHERE seat_id = ?", SEAT_ID);
    }

    @Test
    @DisplayName("solo uno de los dos usuarios consigue la silla")
    void onlyOneReservationWins() throws Exception {
        jdbcTemplate.update("INSERT INTO seat (seat_id, seat_block_id, event_section_id, code, row, position, type, status) "
                + "VALUES (?, 1, 5, 'C1', 1, 1, 'REGULAR', 'AVAILABLE')", SEAT_ID);
        when(eventSectionRepository.findById(anyInt()))
                .thenReturn(Optional.of(EventSection.builder().eventSectionId(5).eventId(9).build()));

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            // Las dos esperan en el latch y se liberan a la vez
            List<Future<Boolean>> results = List.of(pool.submit(attempt(start, 1)), pool.submit(attempt(start, 2)));
            start.countDown();

            long winners = 0;
            for (Future<Boolean> result : results) {
                if (result.get(20, TimeUnit.SECONDS)) {
                    winners++;
                }
            }
            assertThat(winners).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject("SELECT status FROM seat WHERE seat_id = ?", String.class, SEAT_ID))
                    .isEqualTo("RESERVED");
        } finally {
            pool.shutdownNow();
        }
    }

    private Callable<Boolean> attempt(CountDownLatch start, int userId) {
        return () -> {
            start.await();
            try {
                service.reserveSeat(SEAT_ID, userId);
                return true;
            } catch (BusinessException e) {
                assertThat(e.getCode()).isEqualTo("SEAT_NOT_AVAILABLE");
                return false;
            }
        };
    }
}
