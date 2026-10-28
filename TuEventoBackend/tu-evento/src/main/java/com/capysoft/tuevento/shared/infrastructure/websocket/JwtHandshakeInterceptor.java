package com.capysoft.tuevento.shared.infrastructure.websocket;

import com.capysoft.tuevento.shared.infrastructure.security.token.AccessTokenClaims;
import com.capysoft.tuevento.shared.infrastructure.security.token.AccessTokenVerifier;
import com.capysoft.tuevento.shared.infrastructure.security.token.TokenRevocationPort;
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
 * 3. Valida el token con AccessTokenVerifier (JWKS de identity-service, mismo bean que JwtAuthenticationFilter)
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

    private final AccessTokenVerifier accessTokenVerifier;
    private final TokenRevocationPort tokenRevocationPort;

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
        
        log.info("WebSocket handshake attempt from: {}", request.getRemoteAddress());
        log.debug("Request URI: {}", request.getURI());
        
        String token = extractTokenFromQuery(request);

        if (token == null) {
            log.warn("WebSocket handshake rejected: no token provided (URI: {})", request.getURI());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        log.debug("Token extracted, length: {}", token.length());

        // Firma, expiración y que sea un access token (el refresh token se firma igual pero no lleva rol)
        AccessTokenClaims claims = accessTokenVerifier.verify(token).orElse(null);
        if (claims == null) {
            log.warn("WebSocket handshake rejected: invalid, expired or not an access token");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        // Un token revocado (rol quitado, logout, cuenta desactivada) no abre el /ws; si no se puede
        // comprobar, tampoco (fail-closed)
        if (isRevoked(claims)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        // userId disponible en la sesión WS
        attributes.put(USER_ID_ATTRIBUTE, claims.userId());
        log.info("WebSocket handshake accepted for userId={}", claims.userId());

        // Solo aceptar si userId fue extraído correctamente
        boolean accepted = attributes.containsKey(USER_ID_ATTRIBUTE);
        log.info("Handshake result: {}", accepted ? "ACCEPTED" : "REJECTED");
        return accepted;
    }

    private boolean isRevoked(AccessTokenClaims claims) {
        Integer userId = claims.userId();
        try {
            if (tokenRevocationPort.isRevoked(claims)) {
                log.warn("WebSocket handshake rejected: revoked token for userId={}", userId);
                return true;
            }
            return false;
        } catch (TokenRevocationPort.TokenRevocationUnavailableException e) {
            log.error("WebSocket handshake rejected: token revocation store unavailable: {}", e.getMessage());
            return true;
        }
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
