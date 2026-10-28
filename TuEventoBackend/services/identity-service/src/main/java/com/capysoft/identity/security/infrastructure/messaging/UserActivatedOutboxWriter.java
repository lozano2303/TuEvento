package com.capysoft.identity.security.infrastructure.messaging;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.capysoft.identity.security.application.dto.OrganizerRoleRevokedMessage;
import com.capysoft.identity.security.domain.event.UserActivatedEvent;
import com.capysoft.identity.shared.infrastructure.outbox.OutboxEventPublisher;

import lombok.RequiredArgsConstructor;

/**
 * La bienvenida de una cuenta activada la envía Notificaciones (en el monolito): la activación se
 * guarda en el outbox en la misma transacción y llega por {@code tuevento.user} con el tipo
 * {@code UserActivated}. El eventId es estable: una cuenta se activa una sola vez.
 */
@Component
@RequiredArgsConstructor
public class UserActivatedOutboxWriter {

    static final String EVENT_TYPE = "UserActivated";

    private final OutboxEventPublisher outboxEventPublisher;

    /** {@code origin}: el monolito solo reenvía a sus listeners los eventos de identity-service. */
    public record UserActivatedMessage(Integer userId, String alias, String occurredAt, String origin) {
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onUserActivated(UserActivatedEvent event) {
        outboxEventPublisher.publish("user", String.valueOf(event.getUserId()), EVENT_TYPE,
                new UserActivatedMessage(event.getUserId(), event.getAlias(),
                        event.getOccurredAt() == null ? null : event.getOccurredAt().toString(),
                        OrganizerRoleRevokedMessage.ORIGIN),
                UUID.nameUUIDFromBytes((EVENT_TYPE + ":" + event.getUserId()).getBytes(StandardCharsets.UTF_8)));
    }
}
