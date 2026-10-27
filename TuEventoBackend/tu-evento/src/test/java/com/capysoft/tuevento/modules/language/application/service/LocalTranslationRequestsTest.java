package com.capysoft.tuevento.modules.language.application.service;

import com.capysoft.tuevento.modules.language.domain.model.TranslationJob;
import com.capysoft.tuevento.modules.language.domain.repository.TranslationJobRepository;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationJobStatus;
import com.capysoft.tuevento.shared.infrastructure.translation.TranslatableContents;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Los dominios piden, consultan y descartan traducciones a través de este adaptador local.
 */
@DisplayName("LocalTranslationRequests")
class LocalTranslationRequestsTest {

    private final TranslationService service = mock(TranslationService.class);
    private final TranslationJobRepository jobs = mock(TranslationJobRepository.class);
    private final TranslatableContents contents = mock(TranslatableContents.class);
    private final LocalTranslationRequests requests = new LocalTranslationRequests(service, jobs, contents);

    @Test
    @DisplayName("pide la traducción desde el español con el texto original del dominio")
    void requestsFromSpanish() {
        when(contents.sourceTexts("event", 7L)).thenReturn(Map.of("event_name", "Concierto"));

        requests.request("event", 7L);

        verify(service).requestTranslation("event", 7L, "es", Map.of("event_name", "Concierto"));
    }

    @Test
    @DisplayName("devuelve el estado del trabajo o vacío si no hay")
    void jobStatus() {
        when(jobs.findByEntityAndTargetLanguage("event", 7, 2))
                .thenReturn(Optional.of(TranslationJob.builder().status(TranslationJobStatus.PROCESSING).build()));
        when(jobs.findByEntityAndTargetLanguage("event", 7, 3)).thenReturn(Optional.empty());

        assertThat(requests.jobStatus("event", 7L, 2)).contains(TranslationJobStatus.PROCESSING);
        assertThat(requests.jobStatus("event", 7L, 3)).isEmpty();
    }

    @Test
    @DisplayName("bajo demanda: sin trabajo la pide; pendiente o en curso espera; completada o fallida no")
    void ensureRequested() {
        when(jobs.findByEntityAndTargetLanguage("event", 7, 2)).thenReturn(Optional.empty());
        assertThat(requests.ensureRequested("event", 7L, 2)).isTrue();
        verify(service).requestTranslation(eq("event"), eq(7L), eq("es"), anyMap());

        for (TranslationJobStatus status : TranslationJobStatus.values()) {
            when(jobs.findByEntityAndTargetLanguage("event", 8, 2))
                    .thenReturn(Optional.of(TranslationJob.builder().status(status).build()));
            boolean waiting = status == TranslationJobStatus.PENDING || status == TranslationJobStatus.PROCESSING;
            assertThat(requests.ensureRequested("event", 8L, 2)).as(status.name()).isEqualTo(waiting);
        }
        verify(service, never()).requestTranslation(eq("event"), eq(8L), eq("es"), anyMap());
    }

    @Test
    @DisplayName("descarta los trabajos de la entidad y dice cuántos")
    void discards() {
        when(jobs.deleteByEntity("profile", 9)).thenReturn(3);

        assertThat(requests.discard("profile", 9L)).isEqualTo(3);
    }
}
