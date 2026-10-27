package com.capysoft.tuevento.modules.notification.infrastructure.client;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.identitybridge.client.IdentityServiceClient;
import com.capysoft.tuevento.modules.notification.application.port.out.RecipientDirectoryPort;

import lombok.RequiredArgsConstructor;

/** Destinatarios de las notificaciones, leídos de identity-service por su API interna. */
@Component
@RequiredArgsConstructor
class IdentityRecipientDirectoryAdapter implements RecipientDirectoryPort {

    private final IdentityServiceClient identityServiceClient;

    @Override
    public Map<Integer, Recipient> findRecipients(Collection<Integer> userIds) {
        return identityServiceClient.getUserSummaries(userIds).values().stream()
                .collect(Collectors.toMap(s -> s.userId(),
                        s -> new Recipient(s.userId(), s.alias(), s.fullName(), s.email(), s.preferredLanguageId())));
    }
}
