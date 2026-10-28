package com.capysoft.tuevento.modules.wallet.infrastructure.client;

import java.util.List;

import org.springframework.stereotype.Component;

import com.capysoft.tuevento.modules.security.application.port.in.ListUserIdsPort;
import com.capysoft.tuevento.modules.wallet.application.port.out.PlatformUsersPort;

import lombok.RequiredArgsConstructor;

/**
 * Único punto del módulo wallet que conoce a Identidad. Hoy delega en el caso de uso del
 * monolito; al separar Identidad se sustituye por un cliente REST.
 */
@Component
@RequiredArgsConstructor
class IdentityPlatformUsersAdapter implements PlatformUsersPort {

    private final ListUserIdsPort listUserIdsPort;

    @Override
    public long countUsers() {
        return listUserIdsPort.countUsers();
    }

    @Override
    public List<Integer> findUserIdsAfter(int afterUserId, int limit) {
        return listUserIdsPort.findUserIdsAfter(afterUserId, limit);
    }
}
