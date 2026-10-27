package com.capysoft.tuevento.modules.language.infrastructure.translation;

import com.capysoft.tuevento.shared.infrastructure.translation.TranslatableContents;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("LocalTranslationResults")
class LocalTranslationResultsTest {

    @Test
    @DisplayName("entrega la traducción al contenido del dominio")
    void deliversToDomain() {
        TranslatableContents contents = mock(TranslatableContents.class);

        new LocalTranslationResults(contents).deliver("profile", 9L, "pt", Map.of("bio", "Olá"));

        verify(contents).saveMachineTranslation("profile", 9L, "pt", Map.of("bio", "Olá"));
    }
}
