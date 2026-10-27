package com.capysoft.tuevento.modules.notification.infrastructure.client;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.notification.application.port.out.RecipientDirectoryPort;
import com.capysoft.tuevento.modules.security.application.port.in.GetUserSummariesPort;

import lombok.RequiredArgsConstructor;

/**
 * Único punto del módulo notification que lee datos de Identidad. Hoy delega en el caso de uso
 * del monolito; al separar Identidad se sustituye por un cliente REST de
 * {@code GET /internal/users/summary}.
 */
@Component
@RequiredArgsConstructor
class IdentityRecipientDirectoryAdapter implements RecipientDirectoryPort {

    private final GetUserSummariesPort getUserSummariesPort;

    @Override
    public Map<Integer, Recipient> findRecipients(Collection<Integer> userIds) {
        return getUserSummariesPort.getSummaries(userIds).values().stream()
                .collect(Collectors.toMap(s -> s.userId(),
                        s -> new Recipient(s.userId(), s.alias(), s.fullName(), s.email(), s.preferredLanguageId())));
    }
}
