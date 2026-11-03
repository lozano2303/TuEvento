package com.capysoft.commerce.wallet.infrastructure.client;

import java.util.List;

import org.springframework.stereotype.Component;

import com.capysoft.commerce.infrastructure.client.IdentityServiceClient;
import com.capysoft.commerce.wallet.application.port.out.PlatformUsersPort;

import lombok.RequiredArgsConstructor;

/** Usuarios de la plataforma para el crédito masivo de carteras, leídos de identity-service. */
@Component
@RequiredArgsConstructor
class IdentityPlatformUsersAdapter implements PlatformUsersPort {

    private final IdentityServiceClient identityServiceClient;

    @Override
    public long countUsers() {
        return identityServiceClient.countUsers();
    }

    @Override
    public List<Integer> findUserIdsAfter(int afterUserId, int limit) {
        return identityServiceClient.findUserIdsAfter(afterUserId, limit);
    }
}
