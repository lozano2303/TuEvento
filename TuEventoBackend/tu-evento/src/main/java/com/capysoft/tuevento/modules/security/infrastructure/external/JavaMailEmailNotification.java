package com.capysoft.tuevento.modules.security.infrastructure.external;

import com.capysoft.tuevento.modules.security.application.port.out.EmailNotificationPort;
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
        try {
            String htmlTemplate = StreamUtils.copyToString(
                    new ClassPathResource("mail/account-activation.html").getInputStream(),
                    StandardCharsets.UTF_8);

            String htmlContent = htmlTemplate
                    .replace("{{username}}", alias)
                    .replace("{{activationCode}}", activationCode)
                    .replace("{{expirationHours}}", "24");

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject("Tu Evento \u2014 Activa tu cuenta");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            // El código no se registra: quien lea los logs podría usarlo
            log.info("Activation email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send activation email to {}", toEmail, e);
            throw new RuntimeException("Failed to send activation email", e);
        }
    }

    @Override
    public void sendPasswordRecoveryEmail(String toEmail, String alias, String recoveryCode) {
        try {
            String htmlTemplate = StreamUtils.copyToString(
                    new ClassPathResource("mail/password-reset.html").getInputStream(),
                    StandardCharsets.UTF_8);

            String htmlContent = htmlTemplate
                    .replace("{{username}}", alias)
                    .replace("{{verificationCode}}", recoveryCode)
                    .replace("{{expirationMinutes}}", "30");

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject("Tu Evento \u2014 Recuperaci\u00f3n de contrase\u00f1a");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            // El código no se registra: quien lea los logs podría usarlo
            log.info("Password recovery email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password recovery email to {}", toEmail, e);
            throw new RuntimeException("Failed to send password recovery email", e);
        }
    }

    @Override
    public void sendReactivationEmail(String toEmail, String alias, String reactivationToken) {
        try {
            String htmlTemplate = StreamUtils.copyToString(
                    new ClassPathResource("mail/account-reactivation.html").getInputStream(),
                    StandardCharsets.UTF_8);

            String htmlContent = htmlTemplate
                    .replace("{{username}}", alias)
                    .replace("{{reactivationToken}}", reactivationToken)
                    .replace("{{expirationMinutes}}", "30");

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject("Tu Evento \u2014 Reactiva tu cuenta");
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send reactivation email to {}", toEmail, e);
            throw new RuntimeException("Failed to send reactivation email", e);
        }
    }
}
