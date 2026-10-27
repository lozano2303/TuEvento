package com.capysoft.tuevento.modules.notification.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.capysoft.tuevento.modules.notification.application.port.out.RecipientDirectoryPort.Recipient;
import com.capysoft.tuevento.modules.security.application.dto.UserSummary;
import com.capysoft.tuevento.modules.security.application.port.in.GetUserSummariesPort;

@DisplayName("IdentityRecipientDirectoryAdapter (notification → Identidad)")
class IdentityRecipientDirectoryAdapterTest {

    private final GetUserSummariesPort identity = mock(GetUserSummariesPort.class);
    private final IdentityRecipientDirectoryAdapter adapter = new IdentityRecipientDirectoryAdapter(identity);

    @Test
    @DisplayName("entrega alias, nombre, email e idioma de cada destinatario")
    void mapsRecipients() {
        when(identity.getSummaries(List.of(7))).thenReturn(Map.of(
                7, new UserSummary(7, "ana", "ana@test.com", "Ana", 15, 2L)));

        assertThat(adapter.findRecipients(List.of(7)))
                .containsExactly(Map.entry(7, new Recipient(7, "ana", "Ana", "ana@test.com", 2L)));
        assertThat(adapter.findRecipient(7)).contains(new Recipient(7, "ana", "Ana", "ana@test.com", 2L));
    }

    @Test
    @DisplayName("un destinatario desconocido no aparece")
    void unknownRecipient() {
        when(identity.getSummaries(List.of(9))).thenReturn(Map.of());

        assertThat(adapter.findRecipient(9)).isEmpty();
    }
}
