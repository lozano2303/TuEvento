package com.capysoft.identity.shared.infrastructure.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Autenticación entre servicios para {@code /internal/**}: el servicio que llama manda la clave
 * compartida en {@value #HEADER}. Con la clave correcta la petición recibe la autoridad
 * {@value #AUTHORITY}; sin ella queda anónima y la cadena responde 401.
 *
 * <p>Fail-closed: si la clave no está configurada ({@code app.internal.api-key} vacío) ninguna
 * petición se autentica. La comparación es en tiempo constante.
 */
@Slf4j
public class InternalApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Api-Key";
    public static final String AUTHORITY = "INTERNAL_SERVICE";

    private final byte[] expectedKey;

    public InternalApiKeyFilter(String apiKey) {
        this.expectedKey = StringUtils.hasText(apiKey) ? apiKey.getBytes(StandardCharsets.UTF_8) : null;
        if (expectedKey == null) {
            log.warn("app.internal.api-key is not set: every /internal/** request will be rejected");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        if (expectedKey != null && StringUtils.hasText(provided)
                && MessageDigest.isEqual(expectedKey, provided.getBytes(StandardCharsets.UTF_8))) {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "internal-service", null, List.of(new SimpleGrantedAuthority(AUTHORITY))));
        }
        filterChain.doFilter(request, response);
    }
}
