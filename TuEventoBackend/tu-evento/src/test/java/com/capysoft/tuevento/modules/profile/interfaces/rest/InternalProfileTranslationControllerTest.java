package com.capysoft.tuevento.modules.profile.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.capysoft.tuevento.shared.application.translation.TranslationRequests;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationJobStatus;

@DisplayName("InternalProfileTranslationController")
class InternalProfileTranslationControllerTest {

    private final TranslationRequests translations = mock(TranslationRequests.class);
    private final InternalProfileTranslationController controller = new InternalProfileTranslationController(translations);

    private String stateFor(TranslationJobStatus status) {
        when(translations.jobStatus("profile", 30L, 2)).thenReturn(Optional.ofNullable(status));
        return controller.getJobState(30L, 2).getBody().getData().state();
    }

    @Test
    @DisplayName("traduce el estado del núcleo de traducción al que espera identity-service")
    void mapsStates() {
        assertThat(stateFor(null)).isEqualTo("NONE");
        assertThat(stateFor(TranslationJobStatus.PENDING)).isEqualTo("IN_PROGRESS");
        assertThat(stateFor(TranslationJobStatus.PROCESSING)).isEqualTo("IN_PROGRESS");
        assertThat(stateFor(TranslationJobStatus.COMPLETED)).isEqualTo("COMPLETED");
        assertThat(stateFor(TranslationJobStatus.FAILED)).isEqualTo("FAILED");
    }
}
