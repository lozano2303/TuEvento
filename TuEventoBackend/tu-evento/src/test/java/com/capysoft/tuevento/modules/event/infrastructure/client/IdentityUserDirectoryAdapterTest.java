package com.capysoft.tuevento.modules.event.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.capysoft.tuevento.modules.event.application.port.out.UserDirectoryPort.UserRef;
import com.capysoft.tuevento.modules.security.application.dto.UserSummary;
import com.capysoft.tuevento.modules.security.application.port.in.GetUserSummariesPort;

@DisplayName("IdentityUserDirectoryAdapter (event → Identidad)")
class IdentityUserDirectoryAdapterTest {

    private final GetUserSummariesPort identity = mock(GetUserSummariesPort.class);
    private final IdentityUserDirectoryAdapter adapter = new IdentityUserDirectoryAdapter(identity);

    @Test
    @DisplayName("traduce el resumen de Identidad al modelo propio de Catálogo")
    void mapsSummaries() {
        when(identity.getSummaries(List.of(7, 8))).thenReturn(Map.of(
                7, new UserSummary(7, "ana", "ana@test.com", "Ana", 15, 2L),
                8, new UserSummary(8, "bob", null, null, null, null)));

        Map<Integer, UserRef> users = adapter.findUsers(List.of(7, 8));

        assertThat(users).containsEntry(7, new UserRef(7, "Ana", "ana@test.com", 15))
                .containsEntry(8, new UserRef(8, null, null, null));
    }

    @Test
    @DisplayName("un usuario que no existe no aparece y findUser devuelve vacío")
    void unknownUser() {
        when(identity.getSummaries(List.of(9))).thenReturn(Map.of());

        assertThat(adapter.findUser(9)).isEmpty();
        assertThat(adapter.findUser(null)).isEmpty();
    }
}
