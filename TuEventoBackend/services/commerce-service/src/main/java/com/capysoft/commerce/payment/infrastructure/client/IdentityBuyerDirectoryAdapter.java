package com.capysoft.commerce.payment.infrastructure.client;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.capysoft.commerce.infrastructure.client.IdentityServiceClient;
import com.capysoft.commerce.payment.application.port.out.BuyerDirectoryPort;

import lombok.RequiredArgsConstructor;

/** Compradores para Comercio, leídos de identity-service por su API interna. */
@Component
@RequiredArgsConstructor
class IdentityBuyerDirectoryAdapter implements BuyerDirectoryPort {

    private final IdentityServiceClient identityServiceClient;

    @Override
    public Map<Integer, Buyer> findBuyers(Collection<Integer> userIds) {
        return identityServiceClient.getUserSummaries(userIds).values().stream()
                .collect(Collectors.toMap(s -> s.userId(), s -> new Buyer(s.alias(), s.fullName(), s.email())));
    }
}
