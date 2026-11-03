package com.capysoft.commerce.ticket.application.usecase;

import com.capysoft.commerce.ticket.application.dto.response.OrderResponse;
import com.capysoft.commerce.ticket.application.port.out.EventLookupPort;
import com.capysoft.commerce.ticket.application.port.out.EventLookupPort.EventRef;
import com.capysoft.commerce.ticket.domain.model.Money;
import com.capysoft.commerce.ticket.domain.model.Order;
import com.capysoft.commerce.ticket.domain.model.OrderStatus;
import com.capysoft.commerce.ticket.domain.repository.OrderRepository;
import com.capysoft.commerce.ticket.domain.repository.TicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/** Mis órdenes llevan el nombre y el estado del evento, con los eventos publicados primero. */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetMyOrdersUseCaseImpl")
class GetMyOrdersUseCaseImplTest {

    private static final Long USER_ID = 7L;

    @Mock private OrderRepository orderRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private EventLookupPort eventLookupPort;
    @InjectMocks private GetMyOrdersUseCaseImpl useCase;

    private static Order order(long orderId, long eventId, LocalDateTime createdAt) {
        return Order.builder().orderId(orderId).userId(USER_ID).eventId(eventId).status(OrderStatus.PAID)
                .totalAmount(new Money(BigDecimal.TEN, "COP")).createdAt(createdAt).build();
    }

    @Test
    @DisplayName("nombre y estado del evento; publicados primero; evento desconocido sin datos")
    void enrichesWithEventAndSortsPublishedFirst() {
        LocalDateTime now = LocalDateTime.now();
        when(orderRepository.findByUserId(USER_ID)).thenReturn(List.of(
                order(1, 100, now),
                order(2, 200, now.minusDays(1)),
                order(3, 300, now.minusDays(2))));
        when(ticketRepository.findByOrderId(anyLong())).thenReturn(List.of());
        when(eventLookupPort.find(100L)).thenReturn(Optional.of(new EventRef(100L, 30L, "Pasado", "COMPLETED")));
        when(eventLookupPort.find(200L)).thenReturn(Optional.of(new EventRef(200L, 30L, "Concierto", "PUBLISHED")));
        when(eventLookupPort.find(300L)).thenReturn(Optional.empty());

        List<OrderResponse> orders = useCase.execute(USER_ID, null);

        assertThat(orders).extracting(OrderResponse::getOrderId, OrderResponse::getEventName, OrderResponse::getEventStatus)
                .containsExactly(
                        tuple(2L, "Concierto", "PUBLISHED"),
                        tuple(1L, "Pasado", "COMPLETED"),
                        tuple(3L, null, null));
    }
}
