package com.capysoft.tuevento.modules.ticket.interfaces.rest;

import com.capysoft.tuevento.modules.ticket.application.usecase.CancelOrderUseCaseImpl;
import com.capysoft.tuevento.modules.ticket.application.usecase.CheckinTicketUseCaseImpl;
import com.capysoft.tuevento.modules.ticket.application.usecase.CreateOrderWithTicketsUseCaseImpl;
import com.capysoft.tuevento.modules.ticket.application.usecase.GenerateOrderQrUseCaseImpl;
import com.capysoft.tuevento.modules.ticket.application.usecase.GenerateTicketQrUseCaseImpl;
import com.capysoft.tuevento.modules.ticket.application.usecase.GetMyOrdersUseCaseImpl;
import com.capysoft.tuevento.modules.ticket.application.usecase.GetOrderTicketsUseCaseImpl;
import com.capysoft.tuevento.modules.ticket.application.usecase.GetOrderUseCaseImpl;
import com.capysoft.tuevento.modules.ticket.application.usecase.GetTicketUseCaseImpl;
import com.capysoft.tuevento.modules.ticket.application.usecase.GetUserTicketsUseCaseImpl;
import com.capysoft.tuevento.modules.ticket.application.usecase.ScanQrUseCaseImpl;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

/**
 * Los controladores de órdenes y tickets delegan siempre con el usuario autenticado,
 * para que los casos de uso puedan validar que el recurso es suyo.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Order and ticket controllers")
class OrderTicketControllersTest {

    private static final SecurityUser BUYER = new SecurityUser(7, "buyer", "USER");
    private static final SecurityUser ADMIN = new SecurityUser(1, "admin", "ADMIN");

    @Nested
    @DisplayName("OrderController")
    class Orders {

        @Mock private CreateOrderWithTicketsUseCaseImpl createOrderWithTicketsUseCase;
        @Mock private GetOrderUseCaseImpl getOrderUseCase;
        @Mock private GetOrderTicketsUseCaseImpl getOrderTicketsUseCase;
        @Mock private CancelOrderUseCaseImpl cancelOrderUseCase;
        @Mock private GetMyOrdersUseCaseImpl getMyOrdersUseCase;
        @Mock private GenerateOrderQrUseCaseImpl generateOrderQrUseCase;
        @InjectMocks private OrderController controller;

        @Test
        @DisplayName("ver, ver tickets y cancelar → con el id del comprador autenticado")
        void delegatesWithAuthenticatedBuyer() {
            controller.getOrder(100L, BUYER);
            controller.getOrderTickets(100L, BUYER);
            controller.cancelOrder(100L, BUYER);

            verify(getOrderUseCase).execute(100L, 7L);
            verify(getOrderTicketsUseCase).execute(100L, 7L);
            verify(cancelOrderUseCase).execute(100L, 7L);
        }
    }

    @Nested
    @DisplayName("TicketController")
    class Tickets {

        @Mock private GetTicketUseCaseImpl getTicketUseCase;
        @Mock private GetUserTicketsUseCaseImpl getUserTicketsUseCase;
        @Mock private CheckinTicketUseCaseImpl checkinTicketUseCase;
        @Mock private GenerateTicketQrUseCaseImpl generateTicketQrUseCase;
        @Mock private ScanQrUseCaseImpl scanQrUseCase;
        @InjectMocks private TicketController controller;

        @Test
        @DisplayName("ver un ticket → con el id del comprador autenticado")
        void getTicket_delegatesWithAuthenticatedBuyer() {
            controller.getTicket(200L, BUYER);

            verify(getTicketUseCase).execute(200L, 7L);
        }

        @Test
        @DisplayName("tickets de un usuario → pasa quién pide y si es admin")
        void getUserTickets_passesRequesterAndRole() {
            controller.getUserTickets(7L, BUYER);
            controller.getUserTickets(7L, ADMIN);

            verify(getUserTicketsUseCase).executeFor(7L, 7L, false);
            verify(getUserTicketsUseCase).executeFor(7L, 1L, true);
        }
    }
}
