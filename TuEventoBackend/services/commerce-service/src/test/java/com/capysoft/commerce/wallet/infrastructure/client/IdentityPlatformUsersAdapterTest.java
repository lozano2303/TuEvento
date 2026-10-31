package com.capysoft.commerce.wallet.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.capysoft.commerce.infrastructure.client.IdentityServiceClient;

@DisplayName("IdentityPlatformUsersAdapter (wallet → Identidad)")
class IdentityPlatformUsersAdapterTest {

    private final IdentityServiceClient identity = mock(IdentityServiceClient.class);
    private final IdentityPlatformUsersAdapter adapter = new IdentityPlatformUsersAdapter(identity);

    @Test
    @DisplayName("delega el conteo y el recorrido por cursor en Identidad")
    void delegates() {
        when(identity.countUsers()).thenReturn(3L);
        when(identity.findUserIdsAfter(0, 50)).thenReturn(List.of(1, 2, 3));

        assertThat(adapter.countUsers()).isEqualTo(3L);
        assertThat(adapter.findUserIdsAfter(0, 50)).containsExactly(1, 2, 3);
    }
}
