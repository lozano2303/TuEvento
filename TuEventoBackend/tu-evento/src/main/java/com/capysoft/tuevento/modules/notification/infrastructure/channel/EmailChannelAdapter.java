package com.capysoft.tuevento.modules.notification.infrastructure.channel;

import com.capysoft.tuevento.modules.notification.application.port.out.NotificationChannelPort;
import com.capysoft.tuevento.modules.notification.domain.model.DeliveredStatus;
import com.capysoft.tuevento.modules.notification.domain.model.Notification;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationChannelNames;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationUser;
import com.capysoft.tuevento.modules.notification.domain.repository.NotificationUserRepository;
import com.capysoft.tuevento.modules.security.domain.repository.LoginCredentialsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Optional;

/**
 * Canal EMAIL: Spring Mail contra smtp.gmail.com:587 con STARTTLS.
 * Estados: PENDING → SENT si SMTP acepta, FAILED si falla.
 * El correo del destinatario se obtiene del usuario y se copia a notification_user.email_address.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailChannelAdapter implements NotificationChannelPort {

    private final NotificationUserRepository notificationUserRepository;
    private final LoginCredentialsRepository loginCredentialsRepository;
    private final JavaMailSender mailSender;

    @Override
    public String channelName() {
        return NotificationChannelNames.EMAIL;
    }

    @Override
    public void deliver(Notification notification, List<Integer> userIds) {
        for (Integer userId : userIds) {
            String userEmail = null;
            NotificationUser savedRecord = null;
            
            try {
                // Obtener email del LoginCredentials
                userEmail = loginCredentialsRepository.findByUserId(userId)
                        .map(credentials -> credentials.getEmail())
                        .orElse(null);

                if (userEmail == null || userEmail.isBlank()) {
                    log.warn("EMAIL notification skipped: user has no email, userId={}", userId);
                    notificationUserRepository.save(NotificationUser.builder()
                            .notificationId(notification.getNotificationId())
                            .userId(userId)
                            .emailAddress(null)
                            .deliveredStatus(DeliveredStatus.FAILED)
                            .errorMessage("Usuario sin dirección de correo electrónico")
                            .build());
                    continue;
                }

                // Crear registro PENDING
                savedRecord = notificationUserRepository.save(NotificationUser.builder()
                        .notificationId(notification.getNotificationId())
                        .userId(userId)
                        .emailAddress(userEmail)
                        .deliveredStatus(DeliveredStatus.PENDING)
                        .errorMessage(null)
                        .build());

                // Enviar correo
                sendEmail(userEmail, notification.getSubject(), notification.getBody());

                // Actualizar a SENT (preservando el ID)
                NotificationUser updated = NotificationUser.builder()
                        .notificationUserId(savedRecord.getNotificationUserId())
                        .notificationId(savedRecord.getNotificationId())
                        .userId(savedRecord.getUserId())
                        .emailAddress(savedRecord.getEmailAddress())
                        .deliveredStatus(DeliveredStatus.SENT)
                        .errorMessage(null)
                        .readAt(savedRecord.getReadAt())
                        .build();
                notificationUserRepository.save(updated);

                log.debug("EMAIL notification sent: notificationUserId={}, userId={}, email={}",
                        updated.getNotificationUserId(), userId, maskEmail(userEmail));

            } catch (Exception e) {
                log.error("Failed to send EMAIL notification: userId={}, email={}, notificationId={}",
                        userId, maskEmail(userEmail), notification.getNotificationId(), e);

                // Actualizar registro existente a FAILED (si existe) o crear uno nuevo
                try {
                    if (savedRecord != null && savedRecord.getNotificationUserId() != null) {
                        // Ya existe un registro PENDING, actualizarlo a FAILED
                        NotificationUser failed = NotificationUser.builder()
                                .notificationUserId(savedRecord.getNotificationUserId())
                                .notificationId(savedRecord.getNotificationId())
                                .userId(savedRecord.getUserId())
                                .emailAddress(savedRecord.getEmailAddress())
                                .deliveredStatus(DeliveredStatus.FAILED)
                                .errorMessage(truncate(e.getMessage(), 1000))
                                .readAt(savedRecord.getReadAt())
                                .build();
                        notificationUserRepository.save(failed);
                    } else {
                        // No se llegó a crear el registro PENDING, crear uno FAILED
                        NotificationUser failed = NotificationUser.builder()
                                .notificationId(notification.getNotificationId())
                                .userId(userId)
                                .emailAddress(userEmail)
                                .deliveredStatus(DeliveredStatus.FAILED)
                                .errorMessage(truncate(e.getMessage(), 1000))
                                .build();
                        notificationUserRepository.save(failed);
                    }
                } catch (Exception saveError) {
                    log.error("Failed to save FAILED EMAIL notification: userId={}", userId, saveError);
                }
            }
        }
    }

    private void sendEmail(String to, String subject, String body) throws MessagingException {
        if (body != null && body.trim().startsWith("<html")) {
            // HTML email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);  // true = HTML
            mailSender.send(message);
        } else {
            // Plain text email
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        }
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String local = parts[0];
        if (local.length() <= 2) {
            return email;
        }
        return local.substring(0, 2) + "***@" + parts[1];
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}