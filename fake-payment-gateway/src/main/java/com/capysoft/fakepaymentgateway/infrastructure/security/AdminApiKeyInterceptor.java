package com.capysoft.fakepaymentgateway.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Exige una API key en la cabecera X-Api-Key para /admin/**.
 *
 * Es un interceptor de Spring MVC y no un filtro de servlet a propósito: se
 * registra con el patrón /admin/** y Spring lo evalúa sobre la misma ruta
 * decodificada y normalizada que usa para enrutar. Un filtro que compare el
 * requestURI crudo se evade con /%61dmin/... o /admin;x=1/...
 */
@Component
public class AdminApiKeyInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(AdminApiKeyInterceptor.class);
    public static final String API_KEY_HEADER = "X-Api-Key";
    static final String DEFAULT_DEV_KEY = "dev-admin-key-change-in-production";
    static final int MIN_KEY_LENGTH = 16;

    private final byte[] expectedKey;
    private final ObjectMapper objectMapper;

    public AdminApiKeyInterceptor(
        @Value("${gateway.admin.api-key}") String apiKey,
        ObjectMapper objectMapper
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("gateway.admin.api-key (ADMIN_API_KEY) must not be blank");
        }
        if (apiKey.length() < MIN_KEY_LENGTH) {
            throw new IllegalStateException(
                "gateway.admin.api-key (ADMIN_API_KEY) must be at least " + MIN_KEY_LENGTH + " characters");
        }
        if (DEFAULT_DEV_KEY.equals(apiKey)) {
            log.warn("ADMIN_API_KEY is using the development default; set a random value outside local development");
        }
        this.expectedKey = apiKey.getBytes(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // Las preflight de CORS no llevan cabeceras personalizadas
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }

        String provided = request.getHeader(API_KEY_HEADER);
        if (provided != null && MessageDigest.isEqual(expectedKey, provided.getBytes(StandardCharsets.UTF_8))) {
            return true;
        }

        log.warn("Rejected admin request without valid API key: {} {}", request.getMethod(), request.getRequestURI());
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
            "error", "Unauthorized",
            "message", "Missing or invalid " + API_KEY_HEADER + " header",
            "timestamp", Instant.now().toString()
        ));
        return false;
    }
}
