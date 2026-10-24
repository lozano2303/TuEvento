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
 * Impide que un cliente se suscriba a los canales personales de otro usuario.
 *
 * <p>Los canales por usuario usan el userId en la ruta ({@code /topic/users/{id}/session},
 * {@code /topic/notifications/{id}}); sin este filtro cualquier sesión podría leer las
 * notificaciones o eventos de sesión ajenos. El Principal lo crea UserHandshakeHandler
 * con el userId del JWT.
 */
public class UserTopicSubscriptionInterceptor implements ChannelInterceptor {

    private static final Pattern USER_TOPIC =
            Pattern.compile("^/topic/(?:users|notifications)/([^/]+)(?:/.*)?$");

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.SUBSCRIBE) return message;

        String destination = accessor.getDestination();
        if (destination == null) return message;

        Matcher matcher = USER_TOPIC.matcher(destination);
        if (!matcher.matches()) return message;

        Principal principal = accessor.getUser();
        if (principal == null || !matcher.group(1).equals(principal.getName())) {
            throw new MessageDeliveryException("Subscription to another user's channel is not allowed");
        }
        return message;
    }
}
