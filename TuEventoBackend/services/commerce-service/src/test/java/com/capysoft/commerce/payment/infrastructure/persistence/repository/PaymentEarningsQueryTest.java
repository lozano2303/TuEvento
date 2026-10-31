package com.capysoft.commerce.payment.infrastructure.persistence.repository;

import com.capysoft.commerce.payment.application.dto.response.OrganizerEarningsByEventResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consulta de ganancias por evento contra una base real (H2 en modo PostgreSQL del perfil test):
 * "tickets vendidos" cuenta entradas pagadas o usadas, no pagos.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("PaymentJpaRepository.findEarningsByEvents")
class PaymentEarningsQueryTest {

    @Autowired PaymentJpaRepository paymentJpaRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    private void order(long eventId, long orderId, String paymentStatus, String amount, String... ticketStatuses) {
        jdbcTemplate.update("INSERT INTO \"order\" (order_id, user_id, event_id, total_amount, currency, status, "
                + "created_at, updated_at) VALUES (?, 50, ?, ?, 'COP', 'PAID', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                orderId, eventId, amount);
        jdbcTemplate.update("INSERT INTO payment (payment_id, order_id, gateway, gateway_transaction_id, status, amount, "
                + "currency, wallet_amount_applied, created_at, updated_at) "
                + "VALUES (?, ?, 'FAKE', ?, ?, ?, 'COP', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                orderId, orderId, "tx-" + orderId, paymentStatus, amount);
        for (int i = 0; i < ticketStatuses.length; i++) {
            long ticketId = orderId * 100 + i;
            jdbcTemplate.update("INSERT INTO ticket (ticket_id, event_id, user_id, order_id, code, qr_code, status, "
                    + "total_price, currency, created_at, updated_at) "
                    + "VALUES (?, ?, 50, ?, ?, ?, ?, 10000, 'COP', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    ticketId, eventId, orderId, "T" + ticketId, "QR" + ticketId, ticketStatuses[i]);
        }
    }

    @Test
    @DisplayName("una compra de 4 sillas son 4 entradas; las reembolsadas y las de pagos no aprobados no cuentan")
    void countsTicketsNotPayments() {
        order(1, 1, "APPROVED", "40000", "PAID", "PAID", "PAID", "PAID");
        order(1, 2, "APPROVED", "20000", "USED", "REFUNDED");
        order(1, 3, "PENDING", "10000", "PENDING");
        order(2, 4, "APPROVED", "99000", "PAID");

        List<OrganizerEarningsByEventResponse> rows = paymentJpaRepository.findEarningsByEvents(List.of(1L));

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.getEventId()).isEqualTo(1L);
            assertThat(row.getTicketsSold()).isEqualTo(5);
            assertThat(row.getTotalRevenue()).isEqualByComparingTo("60000");
            assertThat(row.getCurrency()).isEqualTo("COP");
        });
    }
}
