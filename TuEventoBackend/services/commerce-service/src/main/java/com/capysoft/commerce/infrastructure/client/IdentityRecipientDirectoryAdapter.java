package com.capysoft.commerce.infrastructure.client;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.capysoft.commerce.infrastructure.client.IdentityServiceClient;
import com.capysoft.commerce.shared.notification.RecipientDirectoryPort;

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
