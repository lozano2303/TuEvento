package com.capysoft.tuevento.shared.infrastructure.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuración base de WebSocket para comunicación en tiempo real.
 * 
 * Características:
 * - STOMP sobre SockJS (fallback automático para navegadores sin soporte nativo de WebSocket)
 * - Compatible con web (Vite/React) y móvil (React Native/Expo)
 * - Autenticación JWT en el handshake (vía JwtHandshakeInterceptor)
 * - Broker simple in-memory (suficiente para MVP; puede escalarse a Redis/RabbitMQ)
 * 
 * Endpoints:
 * - Handshake: /ws (con SockJS: /ws/info, /ws/websocket, etc.)
 * - Subscripciones: /topic/** (ej: /topic/events/{eventId}/seats)
 * - Mensajes desde cliente: /app/** (ej: /app/seats/reserve)
 * 
 * NOTA DE SEGURIDAD: el JWT se pasa como query parameter en la URL del WebSocket
 * porque los navegadores NO permiten headers custom en el handshake inicial.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * Configura el broker de mensajes:
     * - /topic: destinos de subscripción (server → clients broadcast)
     * - /app: prefijo de destinos donde el cliente envía mensajes (client → server)
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Registra el endpoint de handshake de WebSocket con SockJS.
     * 
     * - URL: /ws
     * - CORS: usa el mismo patrón de orígenes permitidos que el resto de la app
     * - SockJS habilitado: proporciona fallbacks (polling, streaming) para clientes
     *   sin soporte nativo de WebSocket
     * - Autenticación: JwtHandshakeInterceptor valida el token antes de establecer
     *   la conexión (vía query param ?token=...)
     * - Compatible con web y móvil usando @stomp/stompjs + sockjs-client
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins.split(","))
                .addInterceptors(jwtHandshakeInterceptor)
                .withSockJS();
    }
}
