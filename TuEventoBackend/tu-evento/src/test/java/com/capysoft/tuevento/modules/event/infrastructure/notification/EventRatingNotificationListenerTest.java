package com.capysoft.tuevento.modules.event.infrastructure.notification;

import com.capysoft.tuevento.modules.event.domain.event.EventRatingAddedEvent;
import com.capysoft.tuevento.modules.event.domain.model.Event;
import com.capysoft.tuevento.modules.event.domain.repository.EventRepository;
import com.capysoft.tuevento.shared.application.notification.NotificationSender;
import com.capysoft.tuevento.shared.application.notification.RecipientDirectoryPort;
import com.capysoft.tuevento.shared.application.notification.RecipientDirectoryPort.Recipient;
import com.capysoft.tuevento.shared.application.notification.SendNotificationCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("EventRatingNotificationListener")
class EventRatingNotificationListenerTest {

    private static final long ORGANIZER = 11L;

    private final NotificationSender sender = mock(NotificationSender.class);
    private final EventRepository events = mock(EventRepository.class);
    private final RecipientDirectoryPort directory = mock(RecipientDirectoryPort.class, CALLS_REAL_METHODS);
    private final EventRatingNotificationListener listener = new EventRatingNotificationListener(sender, events, directory);

    @BeforeEach
    void setUp() {
        when(events.findById(5L)).thenReturn(Optional.of(
                Event.builder().eventId(5L).userId(ORGANIZER).eventName("Concierto").build()));
        org.mockito.Mockito.doReturn(Map.of(7, new Recipient(7, "ana", "Ana Ruiz", "a@t.com", 1L)))
                .when(directory).findRecipients(anyCollection());
    }

    private static EventRatingAddedEvent.EventRatingAddedEventBuilder comment() {
        return EventRatingAddedEvent.builder().ratingId(30L).eventId(5L).userId(7L).comment("¡Muy bueno!")
                .isOrganizer(false);
    }

    private SendNotificationCommand sent() {
        ArgumentCaptor<SendNotificationCommand> command = ArgumentCaptor.forClass(SendNotificationCommand.class);
        verify(sender).send(command.capture());
        return command.getValue();
    }

    @Test
    @DisplayName("un comentario nuevo avisa al organizador, uno por comentario")
    void newCommentNotifiesOrganizer() {
        listener.onEventRatingAdded(comment().build());

        SendNotificationCommand command = sent();
        assertThat(command.getTypeName()).isEqualTo("NEW_COMMENT");
        assertThat(command.getEntityType()).isEqualTo("EVENT");
        assertThat(command.getEntityId()).isEqualTo(5L);
        assertThat(command.getUserIds()).containsExactly(11);
        assertThat(command.getEventName()).isEqualTo("Concierto");
        assertThat(command.getCommenterName()).isEqualTo("Ana Ruiz");
        assertThat(command.getReason()).isEqualTo("¡Muy bueno!");
        assertThat(command.getIdempotencySuffix()).isEqualTo("ratingId:30");
    }

    @Test
    @DisplayName("el organizador comentando su propio evento no se avisa a sí mismo")
    void organizerCommentIsNotNotified() {
        listener.onEventRatingAdded(comment().userId(ORGANIZER).isOrganizer(true).build());

        verify(sender, never()).send(any());
    }

    @Test
    @DisplayName("una respuesta avisa a quien se responde, con su propia clave de idempotencia")
    void replyNotifiesTarget() {
        listener.onEventRatingAdded(comment().parentRatingId(20L).replyToUserId(8L).build());

        SendNotificationCommand command = sent();
        assertThat(command.getTypeName()).isEqualTo("NEW_REPLY");
        assertThat(command.getUserIds()).containsExactly(8);
        assertThat(command.getIdempotencySuffix()).isEqualTo("ratingId:30:reply");
    }

    @Test
    @DisplayName("responderse a sí mismo, o una respuesta sin destinatario, no avisa a nadie")
    void selfReplyIsNotNotified() {
        listener.onEventRatingAdded(comment().parentRatingId(20L).replyToUserId(7L).build());
        listener.onEventRatingAdded(comment().parentRatingId(20L).build());

        verify(sender, never()).send(any());
    }

    @Test
    @DisplayName("el texto se recorta a 200 caracteres y sin nombre se usa 'Un usuario'")
    void truncatesAndFallsBack() {
        org.mockito.Mockito.doReturn(Map.of()).when(directory).findRecipients(anyCollection());

        listener.onEventRatingAdded(comment().comment("x".repeat(250)).build());

        SendNotificationCommand command = sent();
        assertThat(command.getReason()).hasSize(201).endsWith("…");
        assertThat(command.getCommenterName()).isEqualTo("Un usuario");
    }

    @Test
    @DisplayName("sin texto el aviso va con el comentario vacío")
    void nullComment() {
        listener.onEventRatingAdded(comment().comment(null).build());

        assertThat(sent().getReason()).isEmpty();
    }

    @Test
    @DisplayName("un evento que ya no existe no avisa, y un fallo nunca llega al comentario")
    void missingEventAndFailures() {
        listener.onEventRatingAdded(comment().eventId(99L).build());
        verify(sender, never()).send(any());

        doThrow(new IllegalStateException("kafka down")).when(sender).send(any());
        assertThatCode(() -> listener.onEventRatingAdded(comment().build())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("la clave del destinatario es su userId")
    void recipientLookupUsesUserId() {
        listener.onEventRatingAdded(comment().build());

        verify(directory).findRecipients(List.of(7));
    }
}
