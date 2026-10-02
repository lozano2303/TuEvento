package com.capysoft.tuevento.shared.infrastructure.websocket;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * HandshakeHandler personalizado que crea un Principal con el userId extraído del JWT.
 * 
 * Spring necesita un Principal en la sesión WebSocket para poder resolver destinos de usuario
 * con convertAndSendToUser(). Este handler toma el userId de los attributes (puesto por
 * JwtHandshakeInterceptor) y lo convierte en un StompPrincipal.
 */
@Slf4j
public class UserHandshakeHandler extends DefaultHandshakeHandler {

    private static final String USER_ID_ATTRIBUTE = "userId";

    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                      WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        
        // El userId fue extraído del JWT por JwtHandshakeInterceptor
        Integer userId = (Integer) attributes.get(USER_ID_ATTRIBUTE);
        
        if (userId == null) {
            log.warn("No userId found in WebSocket session attributes");
            return null;
        }

        // Crear un Principal simple con el userId como nombre
        StompPrincipal principal = new StompPrincipal(userId.toString());
        log.info("Created WebSocket Principal for userId={}", userId);
        
        return principal;
    }

    /**
     * Implementación simple de Principal que solo contiene el userId como String.
     */
    private static class StompPrincipal implements Principal {
        private final String name;

        public StompPrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
