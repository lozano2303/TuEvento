package com.capysoft.tuevento.shared.infrastructure.websocket;

import com.capysoft.tuevento.modules.security.application.port.out.TokenGeneratorPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * Interceptor de handshake de WebSocket que valida el JWT antes de establecer la conexión.
 * 
 * Flujo:
 * 1. Cliente intenta conectar a ws://host/ws?token=<JWT>
 * 2. beforeHandshake extrae el token del query param
 * 3. Valida el token usando TokenGeneratorPort (mismo bean que JwtAuthenticationFilter)
 * 4. Si válido: extrae userId y lo almacena en attributes (disponible en la sesión WS)
 * 5. Si inválido/ausente: rechaza con 401 Unauthorized
 * 
 * El userId en attributes permite:
 * - Identificar al usuario en @MessageMapping handlers
 * - Asociar subscripciones a usuarios específicos
 * - Auditar operaciones (ej: quién reservó una silla)
 * 
 * NOTA: El token se pasa por query string porque los navegadores NO permiten headers
 * custom en el handshake inicial de WebSocket. Esto queda registrado en logs del servidor
 * (ver nota de seguridad en WebSocketConfig).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final TokenGeneratorPort tokenGenerator;

    private static final String TOKEN_PARAM = "token";
    private static final String USER_ID_ATTRIBUTE = "userId";

    /**
     * Ejecutado antes de completar el handshake de WebSocket.
     * 
     * @param request  petición HTTP del handshake
     * @param response respuesta HTTP (puede modificarse para rechazar)
     * @param wsHandler handler de WebSocket (no usado aquí)
     * @param attributes mapa de atributos que se pasan a la sesión WebSocket
     * @return true para aceptar la conexión, false para rechazar
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {
        
        String token = extractTokenFromQuery(request);

        if (token == null) {
            log.warn("WebSocket handshake rejected: no token provided");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        if (!tokenGenerator.isTokenValid(token)) {
            log.warn("WebSocket handshake rejected: invalid or expired token");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        // Extraer userId del token y almacenarlo en attributes (disponible en la sesión WS)
        tokenGenerator.extractUserId(token).ifPresentOrElse(
            userId -> {
                attributes.put(USER_ID_ATTRIBUTE, userId);
                log.debug("WebSocket handshake accepted for userId={}", userId);
            },
            () -> {
                log.warn("WebSocket handshake rejected: token valid but no userId claim found");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
            }
        );

        // Solo aceptar si userId fue extraído correctamente
        return attributes.containsKey(USER_ID_ATTRIBUTE);
    }

    /**
     * Ejecutado después del handshake (tanto exitoso como fallido).
     * No necesitamos lógica aquí — el método existe por contrato de la interfaz.
     */
    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // Vacío intencionalmente — no hay cleanup necesario
    }

    /**
     * Extrae el JWT del query parameter "token" de la URL del WebSocket.
     * 
     * Ejemplo: ws://localhost:8080/ws?token=eyJhbGciOi...
     * 
     * @param request petición HTTP del handshake
     * @return el token JWT, o null si no está presente
     */
    private String extractTokenFromQuery(ServerHttpRequest request) {
        if (!(request instanceof ServletServerHttpRequest)) {
            return null;
        }

        ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
        String query = servletRequest.getServletRequest().getQueryString();

        if (query == null || query.isEmpty()) {
            return null;
        }

        // Usar UriComponentsBuilder para parsear query params de forma segura
        return UriComponentsBuilder.fromUriString("?" + query)
                .build()
                .getQueryParams()
                .getFirst(TOKEN_PARAM);
    }
}
