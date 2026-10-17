package com.capysoft.tuevento.modules.notification.infrastructure.channel;

import com.capysoft.tuevento.modules.language.domain.model.Language;
import com.capysoft.tuevento.modules.language.domain.model.NotificationTranslation;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.modules.language.domain.repository.NotificationTranslationRepository;
import com.capysoft.tuevento.modules.notification.application.port.out.NotificationChannelPort;
import com.capysoft.tuevento.modules.notification.domain.model.DeliveredStatus;
import com.capysoft.tuevento.modules.notification.domain.model.Notification;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationChannelNames;
import com.capysoft.tuevento.modules.notification.domain.model.NotificationUser;
import com.capysoft.tuevento.modules.notification.domain.repository.NotificationUserRepository;
import com.capysoft.tuevento.modules.profile.domain.model.Profile;
import com.capysoft.tuevento.modules.profile.domain.repository.ProfileRepository;
import com.capysoft.tuevento.modules.security.domain.repository.LoginCredentialsRepository;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
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

    private static final int MAX_TRANSLATION_WAIT_ATTEMPTS = 20; // 20 intentos = 1 minuto
    private static final long TRANSLATION_POLL_INTERVAL_MS = 3000; // 3 segundos

    private final NotificationUserRepository notificationUserRepository;
    private final LoginCredentialsRepository loginCredentialsRepository;
    private final ProfileRepository profileRepository;
    private final NotificationTranslationRepository notificationTranslationRepository;
    private final LanguageRepository languageRepository;
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

                // Obtener idioma preferido del usuario y traducción si es necesario
                TranslatedContent content = getTranslatedContent(notification, userId);
                
                // Enviar correo con contenido (traducido o original)
                sendEmail(userEmail, content.subject, content.body);

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

                log.debug("EMAIL notification sent: notificationUserId={}, userId={}, email={}, language={}",
                        updated.getNotificationUserId(), userId, maskEmail(userEmail), 
                        content.wasTranslated ? content.languageCode : "es (original)");

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

    /**
     * Obtiene el contenido traducido de la notificación según el idioma preferido del usuario.
     * Si el idioma no es español, espera hasta 1 minuto para que la traducción esté lista.
     * Si timeout o error, devuelve contenido original en español.
     */
    private TranslatedContent getTranslatedContent(Notification notification, Integer userId) {
        try {
            // Obtener idioma preferido del usuario
            Optional<Profile> profileOpt = profileRepository.findByUserId(userId);
            if (profileOpt.isEmpty() || profileOpt.get().getPreferredLanguageId() == null) {
                log.debug("User {} has no preferred language, using original Spanish content", userId);
                return new TranslatedContent(notification.getSubject(), notification.getBody(), false, "es");
            }

            Long preferredLanguageId = profileOpt.get().getPreferredLanguageId();
            
            // Obtener información del idioma
            Optional<Language> languageOpt = languageRepository.findById(preferredLanguageId.intValue());
            if (languageOpt.isEmpty()) {
                log.warn("Language {} not found, using original Spanish content", preferredLanguageId);
                return new TranslatedContent(notification.getSubject(), notification.getBody(), false, "es");
            }

            Language language = languageOpt.get();
            
            // Si es español (idioma fuente), no traducir
            if ("es".equalsIgnoreCase(language.getCode())) {
                log.debug("User {} prefers Spanish, using original content", userId);
                return new TranslatedContent(notification.getSubject(), notification.getBody(), false, "es");
            }

            log.info("User {} prefers {}, waiting for translation of notification {}...", 
                    userId, language.getCode(), notification.getNotificationId());

            // Esperar traducción con polling
            NotificationTranslation translation = waitForTranslation(
                    notification.getNotificationId().intValue(), 
                    language.getLanguageId()
            );

            if (translation != null && translation.getStatus() == TranslationStatus.PUBLISHED) {
                log.info("Translation ready for notification {} in {}, sending translated email", 
                        notification.getNotificationId(), language.getCode());
                return new TranslatedContent(
                        translation.getTranslatedSubject(), 
                        translation.getTranslatedBody(), 
                        true, 
                        language.getCode()
                );
            } else {
                log.warn("Translation timeout for notification {} in {}, sending original Spanish content with note", 
                        notification.getNotificationId(), language.getCode());
                // Agregar nota al contenido original
                String noteSubject = notification.getSubject() + " [ES]";
                String noteBody = notification.getBody() + "\n\n---\n" +
                        "Nota: Esta notificación se envió en español debido a que la traducción no estuvo disponible a tiempo.";
                return new TranslatedContent(noteSubject, noteBody, false, "es");
            }

        } catch (Exception e) {
            log.error("Error getting translated content for notification {}, userId={}: {}", 
                    notification.getNotificationId(), userId, e.getMessage(), e);
            return new TranslatedContent(notification.getSubject(), notification.getBody(), false, "es");
        }
    }

    /**
     * Espera hasta 1 minuto (20 intentos x 3s) para que la traducción esté lista.
     * Retorna la traducción si está PUBLISHED, o null si timeout/error.
     */
    private NotificationTranslation waitForTranslation(Integer notificationId, Integer languageId) {
        for (int attempt = 1; attempt <= MAX_TRANSLATION_WAIT_ATTEMPTS; attempt++) {
            try {
                Optional<NotificationTranslation> translationOpt = 
                        notificationTranslationRepository.findByNotificationAndLanguage(notificationId, languageId);

                if (translationOpt.isPresent()) {
                    NotificationTranslation translation = translationOpt.get();
                    if (translation.getStatus() == TranslationStatus.PUBLISHED) {
                        log.debug("Translation found on attempt {}/{}", attempt, MAX_TRANSLATION_WAIT_ATTEMPTS);
                        return translation;
                    }
                    log.debug("Translation status: {}, attempt {}/{}", 
                            translation.getStatus(), attempt, MAX_TRANSLATION_WAIT_ATTEMPTS);
                }

                if (attempt < MAX_TRANSLATION_WAIT_ATTEMPTS) {
                    Thread.sleep(TRANSLATION_POLL_INTERVAL_MS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Translation wait interrupted for notification {} in language {}", 
                        notificationId, languageId);
                return null;
            } catch (Exception e) {
                log.error("Error checking translation status: {}", e.getMessage());
                return null;
            }
        }

        log.warn("Translation timeout after {} attempts for notification {} in language {}", 
                MAX_TRANSLATION_WAIT_ATTEMPTS, notificationId, languageId);
        return null;
    }

    /**
     * Clase interna para encapsular contenido traducido.
     */
    private static class TranslatedContent {
        final String subject;
        final String body;
        final boolean wasTranslated;
        final String languageCode;

        TranslatedContent(String subject, String body, boolean wasTranslated, String languageCode) {
            this.subject = subject;
            this.body = body;
            this.wasTranslated = wasTranslated;
            this.languageCode = languageCode;
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