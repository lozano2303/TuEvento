package com.capysoft.tuevento.modules.event.infrastructure.client;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.event.application.port.out.UserDirectoryPort;
import com.capysoft.tuevento.modules.security.application.port.in.GetUserSummariesPort;

import lombok.RequiredArgsConstructor;

/**
 * Único punto del módulo event que conoce a Identidad. Hoy delega en el caso de uso del
 * monolito; al separar Identidad se sustituye por un cliente REST de
 * {@code GET /internal/users/summary}.
 */
@Component
@RequiredArgsConstructor
class IdentityUserDirectoryAdapter implements UserDirectoryPort {

    private final GetUserSummariesPort getUserSummariesPort;

    @Override
    public Map<Integer, UserRef> findUsers(Collection<Integer> userIds) {
        return getUserSummariesPort.getSummaries(userIds).values().stream()
                .collect(Collectors.toMap(s -> s.userId(),
                        s -> new UserRef(s.userId(), s.fullName(), s.email(), s.storedFileId())));
    }
}
