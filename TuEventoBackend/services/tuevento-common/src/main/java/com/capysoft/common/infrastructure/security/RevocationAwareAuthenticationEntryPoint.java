package com.capysoft.common.infrastructure.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Igual que {@link BearerTokenAuthenticationEntryPoint} (401 con {@code WWW-Authenticate}), salvo
 * cuando la autenticación falló porque no se pudo consultar la lista de revocación: entonces 503,
 * para que el cliente no lo tome como una sesión inválida y cierre sesión.
 */
public class RevocationAwareAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final AuthenticationEntryPoint delegate = new BearerTokenAuthenticationEntryPoint();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, jakarta.servlet.ServletException {
        if (isRevocationUnavailable(authException)) {
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\":false,\"code\":\"AUTH_UNAVAILABLE\","
                    + "\"message\":\"No se pudo validar la sesión. Intenta de nuevo en unos segundos.\"}");
            return;
        }
        delegate.commence(request, response, authException);
    }

    private static boolean isRevocationUnavailable(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof RevocationCheckingJwtDecoder.RevocationUnavailableException) {
                return true;
            }
        }
        return false;
    }
}
