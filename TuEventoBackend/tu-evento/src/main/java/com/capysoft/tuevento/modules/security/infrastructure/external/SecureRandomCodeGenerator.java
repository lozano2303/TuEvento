package com.capysoft.tuevento.modules.security.infrastructure.external;

import com.capysoft.tuevento.modules.security.application.port.out.CodeGeneratorPort;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class SecureRandomCodeGenerator implements CodeGeneratorPort {

    /** Excludes ambiguous characters: 0, O, I, 1 */
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int    CODE_LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generateActivationCode() {
        return generate();
    }

    @Override
    public String generateRecoveryCode() {
        return generate();
    }

    private String generate() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
