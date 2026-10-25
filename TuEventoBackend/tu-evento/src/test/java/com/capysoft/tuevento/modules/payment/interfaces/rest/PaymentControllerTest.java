package com.capysoft.tuevento.modules.payment.interfaces.rest;

import com.capysoft.tuevento.modules.payment.application.dto.request.CreatePaymentRequest;
import com.capysoft.tuevento.modules.payment.application.dto.request.RefundPaymentRequest;
import com.capysoft.tuevento.modules.payment.application.usecase.CreatePaymentUseCaseImpl;
import com.capysoft.tuevento.modules.payment.application.usecase.GetPaymentUseCaseImpl;
import com.capysoft.tuevento.modules.payment.application.usecase.RequestRefundUseCaseImpl;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

/**
 * Pagar, consultar y reembolsar delegan con el usuario autenticado (y su rol en el
 * reembolso), para que los casos de uso validen la propiedad de la orden o del evento.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentController")
class PaymentControllerTest {

    private static final SecurityUser BUYER = new SecurityUser(7, "buyer", "USER");
    private static final SecurityUser ORGANIZER = new SecurityUser(30, "org", "ORGANIZER");

    @Mock private CreatePaymentUseCaseImpl createPaymentUseCase;
    @Mock private GetPaymentUseCaseImpl getPaymentUseCase;
    @Mock private RequestRefundUseCaseImpl requestRefundUseCase;
    @InjectMocks private PaymentController controller;

    @Test
    @DisplayName("pagar y consultar → con el id del comprador autenticado")
    void createAndGet_delegateWithAuthenticatedBuyer() {
        CreatePaymentRequest request = new CreatePaymentRequest(100L, "QR", true);

        controller.createPayment(request, BUYER);
        controller.getPayment(400L, BUYER);

        verify(createPaymentUseCase).execute(request, 7L);
        verify(getPaymentUseCase).execute(400L, 7L);
    }

    @Test
    @DisplayName("reembolsar → pasa el usuario y si es admin")
    void refund_passesRequesterAndRole() {
        controller.refundPayment(400L, new RefundPaymentRequest("Evento reprogramado"), ORGANIZER);

        verify(requestRefundUseCase).execute(400L, "Evento reprogramado", 30L, false);
    }
}
