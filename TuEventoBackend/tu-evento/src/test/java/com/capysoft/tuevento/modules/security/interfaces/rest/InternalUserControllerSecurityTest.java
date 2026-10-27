package com.capysoft.tuevento.modules.security.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.capysoft.tuevento.modules.security.application.dto.UserSummary;
import com.capysoft.tuevento.modules.security.application.port.in.GetUserSummariesPort;
import com.capysoft.tuevento.modules.security.application.port.out.TokenGeneratorPort;
import com.capysoft.tuevento.shared.infrastructure.security.InternalApiKeyFilter;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityConfig;
import com.capysoft.tuevento.shared.interfaces.advice.GlobalExceptionHandler;

/** {@code GET /internal/users/summary} detrás de la SecurityConfig real: solo con la clave entre servicios. */
@WebMvcTest(InternalUserController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:3000",
        "app.internal.api-key=test-internal-key"
})
@DisplayName("InternalUserController security")
class InternalUserControllerSecurityTest {

    private static final String URL = "/internal/users/summary";

    @Autowired MockMvc mockMvc;
    @MockitoBean GetUserSummariesPort getUserSummariesPort;
    @MockitoBean TokenGeneratorPort tokenGenerator;

    @Test
    @DisplayName("con la clave correcta devuelve los resúmenes en el orden pedido y omite los desconocidos")
    void validKeyReturnsSummaries() throws Exception {
        when(getUserSummariesPort.getSummaries(List.of(8, 7, 9))).thenReturn(Map.of(
                7, new UserSummary(7, "ana", "ana@test.com", "Ana", 15, 2L),
                8, new UserSummary(8, "bob", null, null, null, null)));

        mockMvc.perform(get(URL).param("ids", "8,7,9,7").header(InternalApiKeyFilter.HEADER, "test-internal-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].userId").value(8))
                .andExpect(jsonPath("$.data[1].email").value("ana@test.com"));
        verify(getUserSummariesPort).getSummaries(List.of(8, 7, 9));
    }

    @Test
    @DisplayName("sin clave o con una clave incorrecta → 401 y no se consulta nada")
    void missingOrWrongKeyIs401() throws Exception {
        mockMvc.perform(get(URL).param("ids", "7"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INTERNAL_AUTH_REQUIRED"));
        mockMvc.perform(get(URL).param("ids", "7").header(InternalApiKeyFilter.HEADER, "wrong"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(getUserSummariesPort);
    }

    @Test
    @DisplayName("un JWT de usuario, aunque sea de ADMIN, no abre la API interna")
    void userJwtIsNotEnough() throws Exception {
        when(tokenGenerator.isTokenValid("admin-jwt")).thenReturn(true);
        when(tokenGenerator.extractUserId("admin-jwt")).thenReturn(Optional.of(1));
        when(tokenGenerator.extractRole("admin-jwt")).thenReturn(Optional.of("ADMIN"));

        mockMvc.perform(get(URL).param("ids", "7").header("Authorization", "Bearer admin-jwt"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(getUserSummariesPort);
    }

    @Test
    @DisplayName("más de 500 IDs o ninguno → 400")
    void idLimits() throws Exception {
        String tooMany = IntStream.rangeClosed(1, 501).mapToObj(String::valueOf).collect(Collectors.joining(","));

        mockMvc.perform(get(URL).param("ids", tooMany).header(InternalApiKeyFilter.HEADER, "test-internal-key"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TOO_MANY_USER_IDS"));
        mockMvc.perform(get(URL).param("ids", "").header(InternalApiKeyFilter.HEADER, "test-internal-key"))
                .andExpect(status().is4xxClientError());
        verifyNoInteractions(getUserSummariesPort);
    }

    @Test
    @DisplayName("la API interna no queda abierta en el resto de rutas: /api/v1 sigue exigiendo JWT")
    void internalKeyDoesNotOpenPublicApi() throws Exception {
        when(getUserSummariesPort.getSummaries(any())).thenReturn(Map.of());

        mockMvc.perform(get("/api/v1/admin/users").header(InternalApiKeyFilter.HEADER, "test-internal-key"))
                .andExpect(status().isUnauthorized());
    }
}
