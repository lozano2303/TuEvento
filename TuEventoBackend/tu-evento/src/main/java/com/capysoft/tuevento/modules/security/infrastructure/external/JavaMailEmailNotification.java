package com.capysoft.tuevento.modules.security.infrastructure.external;

import com.capysoft.tuevento.modules.security.application.port.out.EmailNotificationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JavaMailEmailNotification implements EmailNotificationPort {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Override
    public void sendActivationEmail(String toEmail, String alias, String activationCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(toEmail);
        message.setSubject("Tu Evento — Activate your account");
        message.setText("""
                Hi %s,

                Welcome to Tu Evento! Please use the following code to activate your account:

                    %s

                This code expires in 24 hours.

                If you did not create an account, you can safely ignore this email.

                — The Tu Evento Team
                """.formatted(alias, activationCode));
        mailSender.send(message);
    }

    @Override
    public void sendPasswordRecoveryEmail(String toEmail, String alias, String recoveryCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(toEmail);
        message.setSubject("Tu Evento — Password recovery");
        message.setText("""
                Hi %s,

                We received a request to reset your password. Use the following code:

                    %s

                This code expires in 30 minutes.

                If you did not request a password reset, please ignore this email.

                — The Tu Evento Team
                """.formatted(alias, recoveryCode));
        mailSender.send(message);
    }
}
