package com.capysoft.geolocation.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.capysoft.geolocation.application.dto.response.SiteResponse;
import com.capysoft.geolocation.application.port.in.CreateSitePort;
import com.capysoft.geolocation.application.port.in.GetCitiesByDepartmentPort;
import com.capysoft.geolocation.application.port.in.GetDepartmentsPort;
import com.capysoft.geolocation.application.port.in.GetNearbySitesPort;
import com.capysoft.geolocation.application.port.in.GetSitePort;
import com.capysoft.geolocation.application.port.in.GetSitesByCityPort;
import com.capysoft.geolocation.application.port.in.GetSitesByDepartmentPort;
import com.capysoft.geolocation.application.port.in.UpdateSitePort;
import com.capysoft.geolocation.infrastructure.security.SecurityConfig;
import com.capysoft.geolocation.interfaces.advice.GlobalExceptionHandler;
import com.capysoft.common.infrastructure.security.SecurityUser;

/**
 * Regresión F1: las sedes se podían crear y editar sin token porque {@code /api/v1/geolocation/**}
 * era público para todos los verbos. Ahora solo la lectura es pública; crear y editar sedes
 * exige ADMIN u ORGANIZER.
 */
@WebMvcTest(GeolocationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("GeolocationController security")
class GeolocationControllerSecurityTest {

    private static final SecurityUser USER = new SecurityUser(5, "ana", "USER");
    private static final SecurityUser ORGANIZER = new SecurityUser(42, "org", "ORGANIZER");
    private static final SecurityUser ADMIN = new SecurityUser(1, "admin", "ADMIN");
    private static final String SITE_BODY = "{\"cityId\":1,\"name\":\"Coliseo\",\"address\":\"Calle 1\","
            + "\"capacity\":500,\"latitude\":4.6,\"longitude\":-74.08}";

    @Autowired private MockMvc mockMvc;

    /** Petición con un access token de Identidad, convertido igual que en producción. */
    private static RequestPostProcessor as(SecurityUser user) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject(user.getAlias())
                .claim("userId", user.getUserId()).claim("role", user.getRole())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        return authentication(SecurityConfig.toAuthentication(jwt));
    }

    // Sin servidor JWKS en el test: la autenticación se inyecta con el mismo conversor que en producción
    @MockitoBean private JwtDecoder jwtDecoder;
    @MockitoBean private GetDepartmentsPort getDepartmentsPort;
    @MockitoBean private GetCitiesByDepartmentPort getCitiesByDepartmentPort;
    @MockitoBean private GetSitesByCityPort getSitesByCityPort;
    @MockitoBean private GetSitesByDepartmentPort getSitesByDepartmentPort;
    @MockitoBean private GetNearbySitesPort getNearbySitesPort;
    @MockitoBean private GetSitePort getSitePort;
    @MockitoBean private CreateSitePort createSitePort;
    @MockitoBean private UpdateSitePort updateSitePort;

    @Test
    @DisplayName("la lectura de departamentos y sedes sigue siendo pública")
    void readsArePublic() throws Exception {
        when(getDepartmentsPort.getDepartments()).thenReturn(List.of());
        when(getSitePort.getSite(3)).thenReturn(SiteResponse.builder().siteId(3).build());

        mockMvc.perform(get("/api/v1/geolocation/departments")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/geolocation/sites/3")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("sin sesión no se puede crear ni editar una sede")
    void anonymousCannotWrite() throws Exception {
        mockMvc.perform(post("/api/v1/geolocation/sites").contentType(MediaType.APPLICATION_JSON).content(SITE_BODY))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/geolocation/sites/3").contentType(MediaType.APPLICATION_JSON).content(SITE_BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(createSitePort, updateSitePort);
    }

    @Test
    @DisplayName("un usuario sin rol de ADMIN u ORGANIZER recibe 403")
    void plainUserIsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/geolocation/sites").with(as(USER))
                        .contentType(MediaType.APPLICATION_JSON).content(SITE_BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/geolocation/sites/3").with(as(USER))
                        .contentType(MediaType.APPLICATION_JSON).content(SITE_BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(createSitePort, updateSitePort);
    }

    @Test
    @DisplayName("ORGANIZER crea y ADMIN edita sedes")
    void organizerAndAdminCanWrite() throws Exception {
        when(createSitePort.createSite(any())).thenReturn(SiteResponse.builder().siteId(9).build());
        when(updateSitePort.updateSite(eq(3), any())).thenReturn(SiteResponse.builder().siteId(3).build());

        mockMvc.perform(post("/api/v1/geolocation/sites").with(as(ORGANIZER))
                        .contentType(MediaType.APPLICATION_JSON).content(SITE_BODY))
                .andExpect(status().isCreated());
        mockMvc.perform(put("/api/v1/geolocation/sites/3").with(as(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(SITE_BODY))
                .andExpect(status().isOk());
    }
}
