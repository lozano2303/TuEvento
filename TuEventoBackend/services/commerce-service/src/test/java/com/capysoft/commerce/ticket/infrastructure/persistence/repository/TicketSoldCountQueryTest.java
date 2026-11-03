package com.capysoft.commerce.ticket.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Sold tickets grouped by event, against a real database (H2 in PostgreSQL mode, test profile). */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TicketRepositoryImpl.class)
@DisplayName("TicketRepositoryImpl.countSoldByEventIds")
class TicketSoldCountQueryTest {

    @Autowired TicketRepositoryImpl ticketRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    private long nextId = 1;

    private void ticket(long eventId, String status) {
        long id = nextId++;
        jdbcTemplate.update("INSERT INTO ticket (ticket_id, event_id, user_id, order_id, code, qr_code, status, "
                + "total_price, currency, created_at, updated_at) "
                + "VALUES (?, ?, 50, 1, ?, ?, ?, 10000, 'COP', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                id, eventId, "T" + id, "QR" + id, status);
    }

    @Test
    @DisplayName("counts only PAID and USED tickets of the requested events")
    void countsPaidAndUsedOfRequestedEvents() {
        ticket(1, "PAID");
        ticket(1, "USED");
        ticket(1, "PENDING");
        ticket(1, "REFUNDED");
        ticket(2, "PAID");
        ticket(3, "PAID");

        assertThat(ticketRepository.countSoldByEventIds(List.of(1L, 2L, 4L)))
                .containsOnly(org.assertj.core.api.Assertions.entry(1L, 2L), org.assertj.core.api.Assertions.entry(2L, 1L));
    }
}
