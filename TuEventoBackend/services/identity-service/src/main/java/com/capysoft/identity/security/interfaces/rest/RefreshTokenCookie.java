package com.capysoft.identity.security.interfaces.rest;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Cookie {@code httpOnly} con el refresh token para el cliente web.
 * <p>
 * La web envía {@code X-Auth-Mode: cookie} en login, refresh y logout: el refresh
 * token viaja solo en esta cookie (JavaScript no puede leerla) y el access token
 * se guarda en memoria. La app móvil no envía la cabecera y sigue recibiendo
 * ambos tokens en el cuerpo para guardarlos en el almacenamiento seguro del dispositivo.
 */
@Component
public class RefreshTokenCookie {

    public static final String NAME = "tuevento_refresh";
    public static final String AUTH_MODE_HEADER = "X-Auth-Mode";
    static final String PATH = "/api/v1/auth";
    static final Duration MAX_AGE = Duration.ofDays(7);

    private final boolean secure;
    private final String sameSite;

    public RefreshTokenCookie(
            @Value("${app.auth.refresh-cookie.secure:true}") boolean secure,
            @Value("${app.auth.refresh-cookie.same-site:Strict}") String sameSite) {
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public static boolean isCookieMode(String authModeHeader) {
        return "cookie".equalsIgnoreCase(authModeHeader);
    }

    public String create(String refreshToken) {
        return build(refreshToken, MAX_AGE).toString();
    }

    public String clear() {
        return build("", Duration.ZERO).toString();
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(PATH)
                .maxAge(maxAge)
                .build();
    }
}
