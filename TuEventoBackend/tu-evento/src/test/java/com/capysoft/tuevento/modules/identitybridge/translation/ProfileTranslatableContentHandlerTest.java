package com.capysoft.tuevento.modules.identitybridge.translation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.capysoft.tuevento.modules.identitybridge.client.IdentityServiceClient;
import com.capysoft.tuevento.shared.domain.exception.ServiceUnavailableException;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;

@DisplayName("ProfileTranslatableContentHandler (translation engine → identity-service)")
class ProfileTranslatableContentHandlerTest {

    private static final ServiceUnavailableException DOWN = new ServiceUnavailableException("IDENTITY_UNAVAILABLE", "down");

    private final IdentityServiceClient identity = mock(IdentityServiceClient.class);
    private final ProfileTranslatableContentHandler handler = new ProfileTranslatableContentHandler(identity);

    @Test
    @DisplayName("reads and saves profile texts and statuses through the identity-service API")
    void delegatesToIdentityService() {
        when(identity.profileTranslatableTexts(30L)).thenReturn(Map.of("biography", "Hola"));
        when(identity.profileTranslationStatus(30L, "en")).thenReturn(Optional.of(TranslationStatus.PUBLISHED));

        assertThat(handler.entityType()).isEqualTo("profile");
        assertThat(handler.loadSourceTexts(30L)).containsEntry("biography", "Hola");
        assertThat(handler.findTranslationStatus(30L, "en")).contains(TranslationStatus.PUBLISHED);
        handler.saveTranslation(30L, "en", Map.of("biography", "Hello"), TranslationSource.MACHINE, TranslationStatus.PUBLISHED);

        verify(identity).saveProfileTranslation(30L, "en", Map.of("biography", "Hello"),
                TranslationSource.MACHINE, TranslationStatus.PUBLISHED);
    }

    @Test
    @DisplayName("with identity-service down there is nothing to translate, but a failed save reaches the engine to retry")
    void identityDown() {
        when(identity.profileTranslatableTexts(30L)).thenThrow(DOWN);
        when(identity.profileTranslationStatus(30L, "en")).thenThrow(DOWN);
        doThrow(DOWN).when(identity).saveProfileTranslation(30L, "en", Map.of(), TranslationSource.MACHINE,
                TranslationStatus.PUBLISHED);

        assertThat(handler.loadSourceTexts(30L)).isEmpty();
        assertThat(handler.findTranslationStatus(30L, "en")).isEmpty();
        assertThatThrownBy(() -> handler.saveTranslation(30L, "en", Map.of(), TranslationSource.MACHINE,
                TranslationStatus.PUBLISHED)).isSameAs(DOWN);
    }
}
