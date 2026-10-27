package com.capysoft.identity.security.application.port.out;

public interface CodeGeneratorPort {

    String generateActivationCode();
    String generateRecoveryCode();
}
