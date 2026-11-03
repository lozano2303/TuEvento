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

import com.capysoft.commerce.ticket.domain.model.OrderStatus;

/**
 * Consulta de sillas ligadas a una orden con el pago en curso, contra una base real
 * (H2 en modo PostgreSQL del perfil test).
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SeatTicketRepositoryImpl.class)
@DisplayName("SeatTicketRepositoryImpl.findSeatIdsByOrderStatus")
class SeatTicketPendingPaymentQueryTest {

    @Autowired SeatTicketRepositoryImpl seatTicketRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    private void order(long orderId, String status, int... seatIds) {
        jdbcTemplate.update("INSERT INTO \"order\" (order_id, user_id, event_id, total_amount, currency, status, "
                + "created_at, updated_at) VALUES (?, 50, 1, 10000, 'COP', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                orderId, status);
        long ticketId = orderId * 100;
        jdbcTemplate.update("INSERT INTO ticket (ticket_id, event_id, user_id, order_id, code, qr_code, status, "
                + "total_price, currency, created_at, updated_at) "
                + "VALUES (?, 1, 50, ?, ?, ?, 'PENDING', 10000, 'COP', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                ticketId, orderId, "T" + ticketId, "QR" + ticketId);
        for (int seatId : seatIds) {
            jdbcTemplate.update("INSERT INTO seat_ticket (seat_id, ticket_id, price) VALUES (?, ?, 5000)",
                    seatId, ticketId);
        }
    }

    @Test
    @DisplayName("solo devuelve las sillas pedidas cuya orden está en PAYMENT_PENDING")
    void returnsOnlyRequestedSeatsWithPendingPayment() {
        order(1, "PAYMENT_PENDING", 10, 11);
        order(2, "PAID", 12);
        order(3, "CANCELLED", 13);
        order(4, "PAYMENT_PENDING", 99);

        assertThat(seatTicketRepository.findSeatIdsByOrderStatus(List.of(10, 11, 12, 13, 14), OrderStatus.PAYMENT_PENDING))
                .containsExactlyInAnyOrder(10, 11);
    }

    @Test
    @DisplayName("una lista vacía no consulta y devuelve vacío")
    void emptyInput_returnsEmpty() {
        assertThat(seatTicketRepository.findSeatIdsByOrderStatus(List.of(), OrderStatus.PAYMENT_PENDING)).isEmpty();
    }
}
