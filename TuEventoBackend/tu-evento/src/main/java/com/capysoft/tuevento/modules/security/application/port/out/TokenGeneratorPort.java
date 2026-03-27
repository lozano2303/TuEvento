package com.capysoft.tuevento.modules.security.application.port.out;

import java.util.Optional;

public interface TokenGeneratorPort {

    String generateAccessToken(Integer userId, String alias, String role);
    String generateRefreshToken(Integer userId);
    Optional<Integer> extractUserId(String token);
    boolean isTokenValid(String token);
}
