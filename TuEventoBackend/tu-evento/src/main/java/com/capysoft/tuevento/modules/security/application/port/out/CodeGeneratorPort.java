package com.capysoft.tuevento.modules.security.application.port.out;

public interface CodeGeneratorPort {

    String generateActivationCode();
    String generateRecoveryCode();
}
