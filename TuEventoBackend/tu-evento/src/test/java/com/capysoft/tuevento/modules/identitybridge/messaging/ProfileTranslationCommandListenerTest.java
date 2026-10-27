package com.capysoft.tuevento.modules.identitybridge.messaging;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.capysoft.tuevento.shared.application.translation.TranslationRequests;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("ProfileTranslationCommandListener (identity-service → motor de traducciones)")
class ProfileTranslationCommandListenerTest {

    private final TranslationRequests jobs = mock(TranslationRequests.class);
    private final ProfileTranslationCommandListener listener =
            new ProfileTranslationCommandListener(jobs, new ObjectMapper());

    @Test
    @DisplayName("REQUEST pide la traducción de la biografía (el núcleo traduce desde el español)")
    void requests() {
        listener.onCommand("{\"action\":\"REQUEST\",\"profileId\":30,\"sourceLanguage\":\"es\"}");
        listener.onCommand("{\"action\":\"REQUEST\",\"profileId\":31}");

        verify(jobs).request("profile", 30L);
        verify(jobs).request("profile", 31L);
    }

    @Test
    @DisplayName("DISCARD descarta los trabajos del texto anterior")
    void discards() {
        listener.onCommand("{\"action\":\"DISCARD\",\"profileId\":30}");

        verify(jobs).discard("profile", 30L);
    }

    @Test
    @DisplayName("acciones desconocidas, sin profileId o mal formadas se ignoran sin romper el consumidor")
    void ignoresInvalid() {
        assertThatNoException().isThrownBy(() -> {
            listener.onCommand("{\"action\":\"DELETE_ALL\",\"profileId\":30}");
            listener.onCommand("{\"action\":\"REQUEST\"}");
            listener.onCommand("not json");
        });
        verifyNoInteractions(jobs);
    }
}
