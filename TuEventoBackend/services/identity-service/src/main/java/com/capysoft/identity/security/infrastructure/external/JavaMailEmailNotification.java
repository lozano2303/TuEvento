package com.capysoft.identity.security.infrastructure.external;

import com.capysoft.identity.security.application.port.out.EmailNotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class JavaMailEmailNotification implements EmailNotificationPort {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Override
    public void sendActivationEmail(String toEmail, String alias, String activationCode) {
        sendTemplatedEmail("activation", toEmail, "Tu Evento — Activa tu cuenta",
                "mail/account-activation.html",
                "{{username}}", alias,
                "{{activationCode}}", activationCode,
                "{{expirationHours}}", "24");
    }

    @Override
    public void sendPasswordRecoveryEmail(String toEmail, String alias, String recoveryCode) {
        sendTemplatedEmail("password recovery", toEmail, "Tu Evento — Recuperación de contraseña",
                "mail/password-reset.html",
                "{{username}}", alias,
                "{{verificationCode}}", recoveryCode,
                "{{expirationMinutes}}", "30");
    }

    @Override
    public void sendReactivationEmail(String toEmail, String alias, String reactivationToken) {
        sendTemplatedEmail("reactivation", toEmail, "Tu Evento — Reactiva tu cuenta",
                "mail/account-reactivation.html",
                "{{username}}", alias,
                "{{reactivationToken}}", reactivationToken,
                "{{expirationMinutes}}", "30");
    }

    /**
     * Carga la plantilla HTML, aplica los reemplazos en orden (pares marcador/valor) y envía el correo.
     * Los valores (códigos, tokens) nunca se registran: quien lea los logs podría usarlos.
     */
    private void sendTemplatedEmail(String kind, String toEmail, String subject, String templatePath,
                                    String... replacements) {
        try {
            String htmlContent = StreamUtils.copyToString(
                    new ClassPathResource(templatePath).getInputStream(), StandardCharsets.UTF_8);
            for (int i = 0; i < replacements.length; i += 2) {
                htmlContent = htmlContent.replace(replacements[i], replacements[i + 1]);
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("{} email sent to {}", kind, toEmail);
        } catch (Exception e) {
            log.error("Failed to send {} email to {}", kind, toEmail, e);
            throw new RuntimeException("Failed to send " + kind + " email", e);
        }
    }
}
