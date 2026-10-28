package com.capysoft.tuevento.modules.section.interfaces.rest;

import com.capysoft.tuevento.modules.section.application.dto.response.SectionTypeResponse;
import com.capysoft.tuevento.modules.section.application.port.in.SectionTypeUseCase;
import com.capysoft.tuevento.modules.section.domain.model.SectionCategory;
import com.capysoft.tuevento.shared.infrastructure.security.token.AccessTokenVerifier;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SectionTypeController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:3000")
@DisplayName("SectionTypeController — filtro por categoría")
class SectionTypeControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AccessTokenVerifier accessTokenVerifier;
    @MockitoBean private SectionTypeUseCase sectionTypeUseCase;

    @Test
    @DisplayName("GET ?category=PALCO es público y devuelve la categoría")
    void filtersByCategory() throws Exception {
        when(sectionTypeUseCase.getAllSectionTypes(SectionCategory.PALCO)).thenReturn(List.of(
                SectionTypeResponse.builder().sectionTypeId(7).name("Palco Diamante").active(true)
                        .category(SectionCategory.PALCO).build()));

        mockMvc.perform(get("/api/v1/section-types").param("category", "PALCO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Palco Diamante"))
                .andExpect(jsonPath("$.data[0].category").value("PALCO"));
    }

    @Test
    @DisplayName("sin filtro lista todo")
    void listsEverything() throws Exception {
        when(sectionTypeUseCase.getAllSectionTypes(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/section-types")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("una categoría inexistente responde 400")
    void unknownCategoryIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/section-types").param("category", "NO_EXISTE"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(sectionTypeUseCase);
    }
}
