package com.capysoft.tuevento.modules.security.infrastructure.external;

import com.capysoft.tuevento.modules.security.application.port.out.EmailNotificationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;

@Component
@RequiredArgsConstructor
public class JavaMailEmailNotification implements EmailNotificationPort {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Override
    public void sendActivationEmail(String toEmail, String alias, String activationCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject("Tu Evento — Activate your account");
            
            String htmlContent = """
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                        <h2 style="color: #6b21a8;">Hi %s,</h2>
                        <p>Welcome to Tu Evento! Please use the following code to activate your account:</p>
                        <div style="background-color: #f3f4f6; padding: 15px; border-radius: 5px; margin: 20px 0;">
                            <p style="font-size: 24px; font-weight: bold; color: #6b21a8; letter-spacing: 3px; text-align: center;">%s</p>
                        </div>
                        <p>Or click the button below to activate your account:</p>
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="http://localhost:3000/verification?email=%s" 
                               style="background: linear-gradient(to right, #7c3aed, #9333ea); 
                                      color: white; 
                                      padding: 15px 30px; 
                                      text-decoration: none; 
                                      border-radius: 8px; 
                                      font-weight: bold; 
                                      display: inline-block;">
                                ACTIVAR CUENTA
                            </a>
                        </div>
                        <p style="color: #666; font-size: 14px;">This code expires in 24 hours.</p>
                        <p style="color: #666; font-size: 14px;">If you did not create an account, you can safely ignore this email.</p>
                        <p style="color: #666; font-size: 14px; margin-top: 30px;">— The Tu Evento Team</p>
                    </div>
                </body>
                </html>
                """.formatted(alias, activationCode, toEmail);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send activation email", e);
        }
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
