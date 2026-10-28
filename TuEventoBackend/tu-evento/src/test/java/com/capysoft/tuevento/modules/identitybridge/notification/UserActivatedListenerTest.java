package com.capysoft.tuevento.modules.identitybridge.notification;

import com.capysoft.tuevento.shared.application.notification.NotificationSender;
import com.capysoft.tuevento.shared.application.notification.RecipientDirectoryPort;
import com.capysoft.tuevento.shared.application.notification.RecipientDirectoryPort.Recipient;
import com.capysoft.tuevento.shared.application.notification.SendNotificationCommand;
import com.capysoft.tuevento.shared.domain.event.identity.UserActivatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("UserActivatedListener")
class UserActivatedListenerTest {

    private final NotificationSender sender = mock(NotificationSender.class);
    private final RecipientDirectoryPort directory = mock(RecipientDirectoryPort.class,
            org.mockito.Mockito.CALLS_REAL_METHODS);
    private final UserActivatedListener listener = new UserActivatedListener(sender, directory);

    private SendNotificationCommand sent() {
        ArgumentCaptor<SendNotificationCommand> command = ArgumentCaptor.forClass(SendNotificationCommand.class);
        verify(sender).send(command.capture());
        return command.getValue();
    }

    private static UserActivatedEvent activated() {
        return UserActivatedEvent.builder().userId(7).alias("ana99").build();
    }

    @Test
    @DisplayName("da la bienvenida con el nombre del perfil")
    void welcomesWithFullName() {
        when(directory.findRecipients(List.of(7))).thenReturn(Map.of(7, new Recipient(7, "ana99", "Ana Ruiz", "a@t.com", 1L)));

        listener.onUserActivated(activated());

        SendNotificationCommand command = sent();
        assertThat(command.getTypeName()).isEqualTo("WELCOME");
        assertThat(command.getEntityType()).isEqualTo("USER");
        assertThat(command.getEntityId()).isEqualTo(7L);
        assertThat(command.getUserIds()).containsExactly(7);
        assertThat(command.getReason()).isEqualTo("Ana Ruiz");
    }

    @Test
    @DisplayName("sin perfil en Identidad usa el alias")
    void fallsBackToAlias() {
        when(directory.findRecipients(anyCollection())).thenReturn(Map.of());

        listener.onUserActivated(activated());

        assertThat(sent().getReason()).isEqualTo("ana99");
    }

    @Test
    @DisplayName("un fallo de Identidad o del envío no llega a la activación")
    void failuresAreSwallowed() {
        when(directory.findRecipients(anyCollection())).thenThrow(new IllegalStateException("identity down"));
        assertThatCode(() -> listener.onUserActivated(activated())).doesNotThrowAnyException();

        doThrow(new IllegalStateException("kafka down")).when(sender).send(any());
        org.mockito.Mockito.doReturn(Map.of()).when(directory).findRecipients(anyCollection());
        assertThatCode(() -> listener.onUserActivated(activated())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("findRecipient sin userId no consulta a Identidad")
    void findRecipientWithoutUser() {
        assertThat(directory.findRecipient(null)).isEmpty();
    }
}
