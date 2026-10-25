package com.capysoft.tuevento.shared.infrastructure.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Limita por IP las peticiones a los endpoints públicos de autenticación
 * (login, registro, recuperación y activación) para frenar fuerza bruta y abuso.
 * <p>
 * Ventana fija de un minuto con un contador en Redis por IP y endpoint. Si Redis
 * no está disponible deja pasar la petición: el bloqueo de cuenta de
 * {@code LoginUseCase} sigue protegiendo el login.
 */
@Slf4j
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    static final String KEY_PREFIX = "rate-limit:auth:";
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private static final Set<String> LIMITED_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/activate",
            "/api/v1/auth/recover-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/resend-activation",
            "/api/v1/auth/reactivate-request",
            "/api/v1/auth/reactivate-confirm");

    private final ObjectProvider<StringRedisTemplate> redisProvider;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final int requestsPerMinute;
    private final int trustedProxies;

    public AuthRateLimitFilter(
            ObjectProvider<StringRedisTemplate> redisProvider,
            ObjectMapper objectMapper,
            @Value("${rate-limit.auth.enabled:true}") boolean enabled,
            @Value("${rate-limit.auth.requests-per-minute:10}") int requestsPerMinute,
            @Value("${rate-limit.auth.trusted-proxies:0}") int trustedProxies) {
        this.redisProvider = redisProvider;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.requestsPerMinute = requestsPerMinute;
        this.trustedProxies = trustedProxies;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled
                || !"POST".equalsIgnoreCase(request.getMethod())
                || !LIMITED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long retryAfterSeconds = consume(KEY_PREFIX + request.getRequestURI() + ":" + resolveClientIp(request));
        if (retryAfterSeconds > 0) {
            reject(response, retryAfterSeconds);
            return;
        }
        chain.doFilter(request, response);
    }

    /**
     * Suma una petición al contador de la ventana actual.
     *
     * @return 0 si la petición está permitida; si no, los segundos que faltan para que se libere
     */
    private long consume(String key) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) return 0;
        try {
            Long count = redis.opsForValue().increment(key);
            if (count == null) return 0;
            if (count == 1) {
                redis.expire(key, WINDOW);
            }
            if (count <= requestsPerMinute) return 0;
            Long ttl = redis.getExpire(key);
            return ttl == null || ttl <= 0 ? WINDOW.toSeconds() : ttl;
        } catch (RuntimeException e) {
            log.warn("Rate limit unavailable, allowing request: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * IP del cliente. Con {@code trusted-proxies = N} proxies propios delante, cada
     * uno añade una entrada al final de {@code X-Forwarded-For}, así que el cliente
     * es la N-ésima empezando por la derecha; las entradas anteriores las puede
     * inventar el propio cliente y se ignoran. Con 0 se usa la conexión directa.
     * <p>
     * Se lee la petición original porque {@code ForwardedHeaderFilter}
     * ({@code forward-headers-strategy: framework}) quita esas cabeceras del wrapper.
     */
    String resolveClientIp(HttpServletRequest request) {
        HttpServletRequest original = unwrap(request);
        String forwardedFor = original.getHeader("X-Forwarded-For");
        if (trustedProxies <= 0 || forwardedFor == null || forwardedFor.isBlank()) {
            return original.getRemoteAddr();
        }
        String[] hops = forwardedFor.split(",");
        int index = Math.max(0, hops.length - trustedProxies);
        return hops[index].trim();
    }

    private static HttpServletRequest unwrap(HttpServletRequest request) {
        ServletRequest current = request;
        while (current instanceof HttpServletRequestWrapper wrapper
                && wrapper.getRequest() instanceof HttpServletRequest) {
            current = wrapper.getRequest();
        }
        return (HttpServletRequest) current;
    }

    private void reject(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.error("TOO_MANY_REQUESTS", "Demasiadas solicitudes. Intenta de nuevo en unos segundos."));
    }
}
