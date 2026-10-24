package com.capysoft.tuevento.shared.infrastructure.websocket;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Autoriza los frames STOMP que envían los clientes.
 *
 * <ul>
 *   <li>SEND: solo a destinos de aplicación ({@code /app/**}). Los clientes nunca publican
 *       directo en el broker; si pudieran, falsificarían avisos de sillas, notificaciones o
 *       eventos de sesión de otros usuarios.</li>
 *   <li>SUBSCRIBE: se rechazan destinos con comodines o rutas no normalizadas, porque el
 *       broker simple los interpreta como patrones Ant ({@code /topic/users/*}/session
 *       recibiría los mensajes de todos). Los canales personales
 *       ({@code /topic/users/{id}/**}, {@code /topic/notifications/{id}}, {@code /topic/role-change/{id}}) solo los puede
 *       escuchar su dueño; el Principal lo crea UserHandshakeHandler con el userId del JWT.</li>
 * </ul>
 */
public class UserTopicSubscriptionInterceptor implements ChannelInterceptor {

    private static final String APP_PREFIX = "/app/";

    private static final Pattern USER_TOPIC =
            Pattern.compile("^/topic/(?:users|notifications|role-change)/([^/]+)(?:/.*)?$");

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) return message;

        String destination = accessor.getDestination();
        switch (accessor.getCommand()) {
            case SEND -> {
                if (destination == null || !destination.startsWith(APP_PREFIX)) {
                    throw new MessageDeliveryException("Clients may only send to /app destinations");
                }
            }
            case SUBSCRIBE -> authorizeSubscription(destination, accessor.getUser());
            default -> { /* CONNECT, UNSUBSCRIBE, DISCONNECT, etc. no llevan destino a validar */ }
        }
        return message;
    }

    private void authorizeSubscription(String destination, Principal principal) {
        if (destination == null || hasPatternOrTraversal(destination)) {
            throw new MessageDeliveryException("Invalid subscription destination");
        }

        Matcher matcher = USER_TOPIC.matcher(destination);
        if (!matcher.matches()) return;

        if (principal == null || !matcher.group(1).equals(principal.getName())) {
            throw new MessageDeliveryException("Subscription to another user's channel is not allowed");
        }
    }

    private static boolean hasPatternOrTraversal(String destination) {
        return destination.contains("*") || destination.contains("?")
                || destination.contains("{") || destination.contains("}")
                || destination.contains("//") || destination.contains("..");
    }
}
