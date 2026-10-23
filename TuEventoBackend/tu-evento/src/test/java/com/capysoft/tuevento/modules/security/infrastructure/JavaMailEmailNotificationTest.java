package com.capysoft.tuevento.modules.security.infrastructure;

import com.capysoft.tuevento.modules.security.infrastructure.external.JavaMailEmailNotification;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Envío de correos con plantilla: destinatario, asunto y reemplazo de marcadores;
 * un fallo del servidor SMTP se propaga como RuntimeException.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JavaMailEmailNotification")
class JavaMailEmailNotificationTest {

    @Mock private JavaMailSender mailSender;

    private JavaMailEmailNotification notification;

    private static final String TO = "ana@gmail.com";

    @BeforeEach
    void setUp() {
        notification = new JavaMailEmailNotification(mailSender);
        ReflectionTestUtils.setField(notification, "from", "noreply@tuevento.com");
        when(mailSender.createMimeMessage())
                .thenAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    @DisplayName("Activación: envía al destinatario con el código en la plantilla")
    void activation_email_is_sent_with_code() throws Exception {
        notification.sendActivationEmail(TO, "ana", "482913");

        MimeMessage sent = captureSent();
        assertEquals(TO, sent.getRecipients(Message.RecipientType.TO)[0].toString());
        assertEquals("Tu Evento — Activa tu cuenta", sent.getSubject());
        String html = htmlOf(sent);
        assertTrue(html.contains("482913"));
        assertFalse(html.contains("{{activationCode}}") || html.contains("{{username}}"));
    }

    @Test
    @DisplayName("Recuperación: envía el código de verificación")
    void password_recovery_email_is_sent_with_code() throws Exception {
        notification.sendPasswordRecoveryEmail(TO, "ana", "775201");

        MimeMessage sent = captureSent();
        assertEquals("Tu Evento — Recuperación de contraseña", sent.getSubject());
        assertTrue(htmlOf(sent).contains("775201"));
    }

    @Test
    @DisplayName("Reactivación: envía el token")
    void reactivation_email_is_sent_with_token() throws Exception {
        notification.sendReactivationEmail(TO, "ana", "tok-abc-123");

        MimeMessage sent = captureSent();
        assertEquals("Tu Evento — Reactiva tu cuenta", sent.getSubject());
        assertTrue(htmlOf(sent).contains("tok-abc-123"));
    }

    @Test
    @DisplayName("Fallo SMTP: se propaga como RuntimeException con el tipo de correo")
    void smtp_failure_is_propagated() {
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> notification.sendActivationEmail(TO, "ana", "482913"));

        assertEquals("Failed to send activation email", ex.getMessage());
        assertInstanceOf(MailSendException.class, ex.getCause());
    }

    private MimeMessage captureSent() throws Exception {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        sent.saveChanges(); // el envío real calcula las cabeceras Content-Type; aquí se fuerza
        return sent;
    }

    /** Recorre el mensaje multipart y devuelve la primera parte HTML. */
    private static String htmlOf(Part part) throws Exception {
        if (part.isMimeType("text/html")) {
            return (String) part.getContent();
        }
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String html = htmlOf(multipart.getBodyPart(i));
                if (html != null) return html;
            }
        }
        return null;
    }
}
