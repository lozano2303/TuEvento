package com.capysoft.tuevento.modules.security.application.port.out;

public interface PasswordEncoderPort {

    String encode(String rawPassword);
    boolean matches(String rawPassword, String encodedPassword);
}
