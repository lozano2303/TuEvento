package com.capysoft.tuevento.modules.wallet.infrastructure.notification;

import com.capysoft.tuevento.modules.wallet.domain.event.WalletCreditedEvent;
import com.capysoft.tuevento.shared.application.notification.NotificationSender;
import com.capysoft.tuevento.shared.application.notification.SendNotificationCommand;
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

@DisplayName("WalletEventListener")
class WalletEventListenerTest {

    private final NotificationSender sender = mock(NotificationSender.class);
    private final WalletEventListener listener = new WalletEventListener(sender);

    @Test
    @DisplayName("un abono a la billetera notifica al usuario con el importe y el motivo")
    void credited() {
        listener.handleWalletCredited(WalletCreditedEvent.builder().walletTransactionId(4L).userId(7)
                .amount(new BigDecimal("2500")).currency("COP").reason("REFUND").build());

        ArgumentCaptor<SendNotificationCommand> command = ArgumentCaptor.forClass(SendNotificationCommand.class);
        verify(sender).send(command.capture());
        assertThat(command.getValue().getTypeName()).isEqualTo("WALLET_CREDITED");
        assertThat(command.getValue().getEntityType()).isEqualTo("WALLET_TRANSACTION");
        assertThat(command.getValue().getEntityId()).isEqualTo(4L);
        assertThat(command.getValue().getUserIds()).containsExactly(7);
        assertThat(command.getValue().getCreditedAmount()).isEqualByComparingTo("2500");
        assertThat(command.getValue().getReason()).isEqualTo("REFUND");
    }

    @Test
    @DisplayName("un fallo al enviar no llega al flujo de la billetera")
    void sendFailureIsSwallowed() {
        doThrow(new IllegalStateException("kafka down")).when(sender).send(any());

        assertThatCode(() -> listener.handleWalletCredited(WalletCreditedEvent.builder().walletTransactionId(4L)
                .userId(7).build())).doesNotThrowAnyException();
    }
}
