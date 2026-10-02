package com.capysoft.tuevento.modules.notification.infrastructure.config;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuración del módulo de notificaciones.
 * - Spring Mail para canal EMAIL
 * - Async habilitado para event listeners
 */
@Configuration
@EnableAsync
public class NotificationConfig {

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private int mailPort;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    /**
     * Configuración de JavaMailSender para Gmail SMTP.
     * Solo se crea el bean si notification.email.enabled=true.
     */
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        if (!emailEnabled || mailUsername.isBlank() || mailPassword.isBlank()) {
            // Si email está deshabilitado o faltan credenciales, 
            // crear un sender básico que fallará gracefully
            mailSender.setHost("localhost");
            mailSender.setPort(25);
            return mailSender;
        }

        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);
        mailSender.setUsername(mailUsername);
        mailSender.setPassword(mailPassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false");
        props.put("mail.smtp.ssl.trust", mailHost);

        return mailSender;
    }
}