package com.capysoft.tuevento.modules.security.application.port.out;

public interface EmailNotificationPort {

    void sendActivationEmail(String toEmail, String alias, String activationCode);
    void sendPasswordRecoveryEmail(String toEmail, String alias, String recoveryCode);
}
