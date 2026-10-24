package com.capysoft.tuevento.shared.infrastructure.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("UserTopicSubscriptionInterceptor")
class UserTopicSubscriptionInterceptorTest {

    private final UserTopicSubscriptionInterceptor interceptor = new UserTopicSubscriptionInterceptor();
    private final MessageChannel channel = mock(MessageChannel.class);
    private static final Principal USER_7 = () -> "7";

    private static Message<byte[]> frame(StompCommand command, String destination, Principal user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (destination != null) accessor.setDestination(destination);
        accessor.setUser(user);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    @DisplayName("mensaje sin cabeceras STOMP → pasa sin validar")
    void nonStompMessage_passesThrough() {
        Message<String> message = MessageBuilder.withPayload("ping").build();

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }

    @Test
    @DisplayName("CONNECT → pasa sin validar destino")
    void connect_passesThrough() {
        Message<byte[]> message = frame(StompCommand.CONNECT, null, null);

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }

    @Test
    @DisplayName("SEND a /app/** → permitido")
    void sendToApp_isAllowed() {
        Message<byte[]> message = frame(StompCommand.SEND, "/app/seats/select", USER_7);

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/topic/users/8/session", "/topic/events/1/seats"})
    @DisplayName("SEND directo al broker → rechazado")
    void sendToBroker_isRejected(String destination) {
        Message<byte[]> message = frame(StompCommand.SEND, destination, USER_7);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    @DisplayName("SEND sin destino → rechazado")
    void sendWithoutDestination_isRejected() {
        Message<byte[]> message = frame(StompCommand.SEND, null, USER_7);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/topic/users/7/session", "/topic/notifications/7", "/topic/role-change/7",
            "/topic/events/12/status"})
    @DisplayName("SUBSCRIBE a canal propio o público → permitido")
    void subscribeOwnOrPublic_isAllowed(String destination) {
        Message<byte[]> message = frame(StompCommand.SUBSCRIBE, destination, USER_7);

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/topic/users/8/session", "/topic/notifications/8", "/topic/role-change/8"})
    @DisplayName("SUBSCRIBE al canal personal de otro usuario → rechazado")
    void subscribeOtherUser_isRejected(String destination) {
        Message<byte[]> message = frame(StompCommand.SUBSCRIBE, destination, USER_7);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    @DisplayName("SUBSCRIBE a canal personal sin Principal → rechazado")
    void subscribeWithoutPrincipal_isRejected() {
        Message<byte[]> message = frame(StompCommand.SUBSCRIBE, "/topic/users/7/session", null);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/topic/users/*/session", "/topic/users/?/session", "/topic/users/{id}/session",
            "/topic//users/7", "/topic/users/7/../8/session"})
    @DisplayName("SUBSCRIBE con comodines o rutas no normalizadas → rechazado")
    void subscribeWithPattern_isRejected(String destination) {
        Message<byte[]> message = frame(StompCommand.SUBSCRIBE, destination, USER_7);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    @DisplayName("SUBSCRIBE sin destino → rechazado")
    void subscribeWithoutDestination_isRejected() {
        Message<byte[]> message = frame(StompCommand.SUBSCRIBE, null, USER_7);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessageDeliveryException.class);
    }
}
