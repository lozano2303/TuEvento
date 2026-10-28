package com.capysoft.tuevento.modules.event.infrastructure.client;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.event.application.port.out.UserDirectoryPort;
import com.capysoft.tuevento.modules.identitybridge.client.IdentityServiceClient;

import lombok.RequiredArgsConstructor;

/** Organizadores para Catálogo, leídos de identity-service por su API interna. */
@Component
@RequiredArgsConstructor
class IdentityUserDirectoryAdapter implements UserDirectoryPort {

    private final IdentityServiceClient identityServiceClient;

    @Override
    public Map<Integer, UserRef> findUsers(Collection<Integer> userIds) {
        return identityServiceClient.getUserSummaries(userIds).values().stream()
                .collect(Collectors.toMap(s -> s.userId(),
                        s -> new UserRef(s.userId(), s.fullName(), s.email(), s.storedFileId())));
    }
}
