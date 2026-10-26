package com.capysoft.tuevento.modules.security.infrastructure.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import com.capysoft.tuevento.modules.security.infrastructure.external.JwtTokenGenerator;

import jakarta.persistence.Column;

/**
 * Regresión: con RS256 los JWT superaron los 512 caracteres de auth_session.access_token y
 * refresh_token.token, y el login fallaba al guardar la sesión en PostgreSQL. Los tests con H2
 * no lo detectaban. Las columnas (migración 107 y entidades) deben admitir un token real.
 */
@DisplayName("Columnas de tokens de sesión — caben los JWT RS256")
class SessionTokenColumnLengthTest {

    private static JwtTokenGenerator generator() {
        JwtTokenGenerator generator = new JwtTokenGenerator();
        ReflectionTestUtils.setField(generator, "privateKeyResource", new ClassPathResource("keys/none.pem"));
        ReflectionTestUtils.setField(generator, "ephemeralKeysAllowed", true);
        ReflectionTestUtils.setField(generator, "accessTokenExpiration", 900_000L);
        ReflectionTestUtils.setField(generator, "refreshTokenExpiration", 604_800_000L);
        ReflectionTestUtils.invokeMethod(generator, "init");
        return generator;
    }

    private static int columnLength(Class<?> entity, String field) throws NoSuchFieldException {
        return entity.getDeclaredField(field).getAnnotation(Column.class).length();
    }

    @Test
    @DisplayName("un access token RS256 con alias y rol largos cabe en auth_session.access_token")
    void accessTokenFits() throws Exception {
        String token = generator().generateAccessToken(Integer.MAX_VALUE, "a".repeat(50), "ORGANIZER");

        assertThat(token.length()).isGreaterThan(512);
        assertThat(columnLength(AuthSessionEntity.class, "accessToken")).isGreaterThanOrEqualTo(token.length());
    }

    @Test
    @DisplayName("un refresh token RS256 cabe en refresh_token.token")
    void refreshTokenFits() throws Exception {
        String token = generator().generateRefreshToken(Integer.MAX_VALUE);

        assertThat(columnLength(RefreshTokenEntity.class, "token")).isGreaterThanOrEqualTo(token.length());
    }
}
