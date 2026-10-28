package com.capysoft.notification.infrastructure.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.capysoft.notification.application.dto.NotificationRecipient;
import com.capysoft.notification.application.port.out.NotificationEventsPort;
import com.capysoft.notification.application.port.out.NotificationTranslationReader;
import com.capysoft.notification.application.port.out.NotificationTranslationReader.TranslationView;
import com.capysoft.notification.domain.model.DeliveredStatus;
import com.capysoft.notification.domain.model.Notification;
import com.capysoft.notification.domain.model.NotificationUser;
import com.capysoft.notification.domain.repository.NotificationUserRepository;
import com.capysoft.notification.infrastructure.persistence.repository.JpaNotificationTypeRepository;

@DisplayName("Canales de entrega del servicio")
class NotificationChannelsTest {

    private final NotificationUserRepository notificationUsers = mock(NotificationUserRepository.class);

    private static Notification notification() {
        return Notification.builder().notificationId(42L).notificationTypeId(3L).entityType("PAYMENT").entityId(9L)
                .subject("Pago aprobado").body("Tu pago fue aprobado").build();
    }

    private NotificationUser saving() {
        when(notificationUsers.save(any())).thenAnswer(inv -> {
            NotificationUser u = inv.getArgument(0);
            return NotificationUser.builder().notificationUserId(100L).notificationId(u.getNotificationId())
                    .userId(u.getUserId()).emailAddress(u.getEmailAddress()).deliveredStatus(u.getDeliveredStatus())
                    .build();
        });
        return null;
    }

    @Nested
    @DisplayName("IN_APP")
    class InApp {

        private final NotificationEventsPort events = mock(NotificationEventsPort.class);
        private final JpaNotificationTypeRepository types = mock(JpaNotificationTypeRepository.class);
        private final InAppChannelAdapter adapter = new InAppChannelAdapter(notificationUsers, events, types);

        @Test
        @DisplayName("guarda la notificación del usuario como DELIVERED y publica el push con su contenido")
        void savesAndPublishesPush() {
            saving();
            when(types.findById(3L)).thenReturn(Optional.empty());

            adapter.deliver(notification(), List.of(NotificationRecipient.withoutContact(7)));

            ArgumentCaptor<NotificationUser> saved = ArgumentCaptor.forClass(NotificationUser.class);
            verify(notificationUsers).save(saved.capture());
            assertThat(saved.getValue().getDeliveredStatus()).isEqualTo(DeliveredStatus.DELIVERED);
            ArgumentCaptor<Map<String, Object>> body = ArgumentCaptor.forClass(Map.class);
            verify(events).push(eq(7), body.capture());
            assertThat(body.getValue()).containsEntry("notificationUserId", 100L)
                    .containsEntry("subject", "Pago aprobado").containsEntry("read", false);
        }

        @Test
        @DisplayName("si falla el push, la notificación queda guardada igual")
        void pushFailure_keepsNotification() {
            saving();
            when(types.findById(3L)).thenReturn(Optional.empty());
            org.mockito.Mockito.doThrow(new IllegalStateException("kafka down")).when(events).push(eq(7), anyMap());

            adapter.deliver(notification(), List.of(NotificationRecipient.withoutContact(7)));

            verify(notificationUsers).save(any());
        }
    }

    @Nested
    @DisplayName("EMAIL")
    class Email {

        private final NotificationTranslationReader translations = mock(NotificationTranslationReader.class);
        private final JavaMailSender mail = mock(JavaMailSender.class);
        private final EmailChannelAdapter adapter = new EmailChannelAdapter(notificationUsers, translations, mail);

        @Test
        @DisplayName("sin email del destinatario no envía nada y lo registra como FAILED")
        void noEmail_recordsFailure() {
            saving();

            adapter.deliver(notification(), List.of(NotificationRecipient.withoutContact(7)));

            ArgumentCaptor<NotificationUser> saved = ArgumentCaptor.forClass(NotificationUser.class);
            verify(notificationUsers).save(saved.capture());
            assertThat(saved.getValue().getDeliveredStatus()).isEqualTo(DeliveredStatus.FAILED);
            verify(mail, never()).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("envía al email que llega en el mensaje, traducido si la traducción está publicada")
        void sendsTranslatedEmail() {
            saving();
            when(translations.languageCode(2L)).thenReturn(Optional.of("en"));
            when(translations.find(42L, 2L)).thenReturn(Optional.of(
                    new TranslationView("en", "Payment approved", "Your payment was approved", "PUBLISHED", "AUTOMATIC")));

            adapter.deliver(notification(), List.of(new NotificationRecipient(7, "ana@test.com", 2L)));

            ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mail).send(sent.capture());
            assertThat(sent.getValue().getTo()).containsExactly("ana@test.com");
            assertThat(sent.getValue().getSubject()).isEqualTo("Payment approved");
            ArgumentCaptor<NotificationUser> saved = ArgumentCaptor.forClass(NotificationUser.class);
            verify(notificationUsers, org.mockito.Mockito.times(2)).save(saved.capture());
            assertThat(saved.getAllValues().get(1).getDeliveredStatus()).isEqualTo(DeliveredStatus.SENT);
        }

        @Test
        @DisplayName("si el usuario prefiere español, envía el texto original sin esperar traducción")
        void spanishUser_sendsOriginal() {
            saving();
            when(translations.languageCode(1L)).thenReturn(Optional.of("es"));

            adapter.deliver(notification(), List.of(new NotificationRecipient(7, "ana@test.com", 1L)));

            ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mail).send(sent.capture());
            assertThat(sent.getValue().getSubject()).isEqualTo("Pago aprobado");
            verify(translations, never()).find(any(), any());
        }
    }
}
