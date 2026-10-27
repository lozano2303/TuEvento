package com.capysoft.tuevento.modules.language.application.service;

import com.capysoft.tuevento.modules.language.application.port.out.TranslationPort;
import com.capysoft.tuevento.modules.language.application.port.out.TranslationResults;
import com.capysoft.tuevento.modules.language.domain.model.Language;
import com.capysoft.tuevento.modules.language.domain.model.TranslationJob;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.modules.language.domain.repository.TranslationJobRepository;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationJobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TranslationProcessorTest {

    @Mock
    private TranslationJobRepository translationJobRepository;

    @Mock
    private LanguageRepository languageRepository;

    @Mock
    private TranslationPort translationPort;

    @Mock
    private TextMaskingService maskingService;

    @Mock
    private TranslationResults translationResults;

    private TranslationProcessor translationProcessor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        translationProcessor = new TranslationProcessor(
                translationJobRepository,
                languageRepository,
                translationPort,
                maskingService,
                translationResults
        );

        // Set maxBackoffMinutes via reflection
        ReflectionTestUtils.setField(translationProcessor, "maxBackoffMinutes", 60);
    }

    /** Un trabajo pendiente del evento 100 al inglés, reclamado, con su texto original. */
    private TranslationJob pendingJob(Map<String, String> sourceTexts) {
        TranslationJob job = createMockJob(1, "event", 100, 1, 2);
        when(job.getSourceTexts()).thenReturn(sourceTexts);
        when(translationJobRepository.findByEntityAndStatus("event", 100, TranslationJobStatus.PENDING))
                .thenReturn(List.of(job));
        when(translationJobRepository.claimJob(1)).thenReturn(true);
        Language sourceLanguage = createMockLanguage(1, "es");
        Language targetLanguage = createMockLanguage(2, "en");
        when(languageRepository.findById(1)).thenReturn(Optional.of(sourceLanguage));
        when(languageRepository.findById(2)).thenReturn(Optional.of(targetLanguage));
        return job;
    }

    @Test
    void testTranslatesStoredTextsAndDeliversThem() throws Exception {
        // Given
        TranslationJob job = pendingJob(Map.of("title", "Hola"));
        when(maskingService.maskText("Hola"))
                .thenReturn(new TextMaskingService.MaskingResult("Hola", Map.of(), Map.of()));
        when(translationPort.translate("Hola", "es", "en")).thenReturn("Hello");
        when(maskingService.unmaskText("Hello", Map.of(), Map.of())).thenReturn("Hello");

        // When
        translationProcessor.processTranslationJobs("event", 100L);

        // Then
        verify(translationResults).deliver("event", 100L, "en", Map.of("title", "Hello"));
        verify(job).markCompleted();
        verify(translationJobRepository).save(job);
    }

    @Test
    void testJobWithoutTextsCompletesWithoutTranslating() throws Exception {
        // Given
        TranslationJob job = pendingJob(null);

        // When
        translationProcessor.processTranslationJobs("event", 100L);

        // Then
        verify(translationPort, never()).translate(anyString(), anyString(), anyString());
        verify(translationResults, never()).deliver(anyString(), anyLong(), anyString(), anyMap());
        verify(job).markCompleted();
    }

    @Test
    void testUnknownLanguageFailsTheJob() throws Exception {
        // Given
        TranslationJob job = pendingJob(Map.of("title", "Hola"));
        when(languageRepository.findById(2)).thenReturn(Optional.empty());

        // When
        translationProcessor.processTranslationJobs("event", 100L);

        // Then
        verify(job).markFailed(contains("language not found"), eq(60));
        verify(translationResults, never()).deliver(anyString(), anyLong(), anyString(), anyMap());
    }

    @Test
    void testAtomicClaimReturnsZeroRows() throws Exception {
        // Given
        TranslationJob job = createMockJob(1, "event", 100, 1, 2);

        when(translationJobRepository.findByEntityAndStatus("event", 100, TranslationJobStatus.PENDING))
                .thenReturn(List.of(job));
        when(translationJobRepository.claimJob(1)).thenReturn(false); // Claim failed

        // When
        translationProcessor.processTranslationJobs("event", 100L);

        // Then
        verify(translationJobRepository).claimJob(1);
        verify(languageRepository, never()).findById(any());
        verify(translationPort, never()).translate(anyString(), anyString(), anyString());
        verify(translationResults, never()).deliver(anyString(), anyLong(), anyString(), anyMap());
        verify(translationJobRepository, never()).save(any(TranslationJob.class));
    }

    @Test
    void testMissingPlaceholderAfterTranslation() throws Exception {
        // Given
        TranslationJob job = pendingJob(Map.of("title", "Welcome to Tu Evento, {name}!"));

        TextMaskingService.MaskingResult maskingResult = new TextMaskingService.MaskingResult(
                "Welcome to {{T_1}}, {{V_1}}!",
                Map.of("{{T_1}}", "Tu Evento"),
                Map.of("{{V_1}}", "{name}")
        );

        when(maskingService.maskText("Welcome to Tu Evento, {name}!"))
                .thenReturn(maskingResult);
        when(translationPort.translate("Welcome to {{T_1}}, {{V_1}}!", "es", "en"))
                .thenReturn("Bienvenido"); // Missing placeholders
        when(maskingService.unmaskText(eq("Bienvenido"), anyMap(), anyMap()))
                .thenThrow(new TextMaskingService.MaskingException("Missing placeholder in translated text: {{T_1}}"));
        // El respaldo sin enmascarar también pierde la variable {name} → el job debe fallar
        when(translationPort.translate("Welcome to Tu Evento, {name}!", "es", "en"))
                .thenReturn("Bienvenido");

        // When
        translationProcessor.processTranslationJobs("event", 100L);

        // Then
        verify(translationJobRepository).claimJob(1);
        verify(translationPort).translate("Welcome to {{T_1}}, {{V_1}}!", "es", "en");
        verify(translationPort).translate("Welcome to Tu Evento, {name}!", "es", "en");
        verify(maskingService).unmaskText("Bienvenido",
                Map.of("{{T_1}}", "Tu Evento"),
                Map.of("{{V_1}}", "{name}"));
        verify(translationResults, never()).deliver(anyString(), anyLong(), anyString(), anyMap());
        verify(job).markFailed(contains("{{T_1}}"), eq(60));
        verify(translationJobRepository).save(job);
    }

    @Test
    void testMissingPlaceholderFallsBackToUnmaskedTranslation() throws Exception {
        // Given
        TranslationJob job = pendingJob(Map.of("title", "Tu Evento aprobó a {name}"));

        TextMaskingService.MaskingResult maskingResult = new TextMaskingService.MaskingResult(
                "{{T_1}} aprobó a {{V_1}}",
                Map.of("{{T_1}}", "Tu Evento"),
                Map.of("{{V_1}}", "{name}")
        );

        when(maskingService.maskText("Tu Evento aprobó a {name}")).thenReturn(maskingResult);
        // El motor elimina el marcador del inicio de la frase
        when(translationPort.translate("{{T_1}} aprobó a {{V_1}}", "es", "en"))
                .thenReturn("approved {{V_1}}");
        when(maskingService.unmaskText(eq("approved {{V_1}}"), anyMap(), anyMap()))
                .thenThrow(new TextMaskingService.MaskingException("Missing placeholder in translated text: {{T_1}}"));
        // Respaldo sin enmascarar conserva la variable → se acepta
        when(translationPort.translate("Tu Evento aprobó a {name}", "es", "en"))
                .thenReturn("Tu Evento approved {name}");

        // When
        translationProcessor.processTranslationJobs("event", 100L);

        // Then
        verify(translationResults).deliver(eq("event"), eq(100L), eq("en"),
                argThat(m -> "Tu Evento approved {name}".equals(m.get("title"))));
        verify(job, never()).markFailed(anyString(), anyInt());
    }

    private TranslationJob createMockJob(Integer jobId, String entityType, Integer entityId,
                                       Integer sourceLanguageId, Integer targetLanguageId) {
        TranslationJob job = mock(TranslationJob.class);
        when(job.getJobId()).thenReturn(jobId);
        when(job.getEntityType()).thenReturn(entityType);
        when(job.getEntityId()).thenReturn(entityId);
        when(job.getSourceLanguageId()).thenReturn(sourceLanguageId);
        when(job.getTargetLanguageId()).thenReturn(targetLanguageId);
        return job;
    }

    private Language createMockLanguage(Integer id, String code) {
        Language language = mock(Language.class);
        when(language.getLanguageId()).thenReturn(id);
        when(language.getCode()).thenReturn(code);
        return language;
    }
}
