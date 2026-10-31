package com.capysoft.commerce.payment.infrastructure.notification;

import com.capysoft.commerce.payment.domain.event.PaymentApprovedEvent;
import com.capysoft.commerce.payment.domain.event.PaymentRefundedEvent;
import com.capysoft.commerce.shared.notification.NotificationSender;
import com.capysoft.commerce.shared.notification.SendNotificationCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("PaymentEventListener")
class PaymentEventListenerTest {

    private final NotificationSender sender = mock(NotificationSender.class);
    private final PaymentEventListener listener = new PaymentEventListener(sender);

    private SendNotificationCommand sent() {
        ArgumentCaptor<SendNotificationCommand> command = ArgumentCaptor.forClass(SendNotificationCommand.class);
        verify(sender).send(command.capture());
        return command.getValue();
    }

    @Test
    @DisplayName("un pago aprobado notifica al comprador con los importes del pago")
    void approved() {
        listener.handlePaymentApproved(PaymentApprovedEvent.builder().paymentId(9L).userId(7)
                .walletAmount(new BigDecimal("1000")).gatewayAmount(new BigDecimal("14000")).currency("COP").build());

        SendNotificationCommand command = sent();
        assertThat(command.getTypeName()).isEqualTo("PAYMENT_APPROVED");
        assertThat(command.getEntityType()).isEqualTo("PAYMENT");
        assertThat(command.getEntityId()).isEqualTo(9L);
        assertThat(command.getUserIds()).containsExactly(7);
        assertThat(command.getWalletAmount()).isEqualByComparingTo("1000");
        assertThat(command.getGatewayAmount()).isEqualByComparingTo("14000");
        assertThat(command.getCurrency()).isEqualTo("COP");
    }

    @Test
    @DisplayName("un reembolso notifica al comprador con los importes devueltos")
    void refunded() {
        listener.handlePaymentRefunded(PaymentRefundedEvent.builder().paymentId(9L).userId(7)
                .walletAmount(BigDecimal.ZERO).gatewayAmount(new BigDecimal("15000")).currency("COP").build());

        SendNotificationCommand command = sent();
        assertThat(command.getTypeName()).isEqualTo("PAYMENT_REFUNDED");
        assertThat(command.getGatewayAmount()).isEqualByComparingTo("15000");
    }

    @Test
    @DisplayName("un fallo al enviar no llega al flujo del pago")
    void sendFailureIsSwallowed() {
        doThrow(new IllegalStateException("kafka down")).when(sender).send(any());

        assertThatCode(() -> listener.handlePaymentApproved(PaymentApprovedEvent.builder().paymentId(1L).userId(7).build()))
                .doesNotThrowAnyException();
        assertThatCode(() -> listener.handlePaymentRefunded(PaymentRefundedEvent.builder().paymentId(1L).userId(7).build()))
                .doesNotThrowAnyException();
    }
}
