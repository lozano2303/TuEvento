package com.capysoft.tuevento.modules.language.application.service;

import com.capysoft.tuevento.modules.language.application.port.out.TranslatableContentHandler;
import com.capysoft.tuevento.modules.language.application.port.out.TranslationPort;
import com.capysoft.tuevento.modules.language.domain.model.Language;
import com.capysoft.tuevento.modules.language.domain.model.TranslationJob;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.modules.language.domain.repository.TranslationJobRepository;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationJobStatus;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
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
    private TranslatableContentHandler contentHandler;
    
    private TranslationProcessor translationProcessor;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        Map<String, TranslatableContentHandler> handlerRegistry = new HashMap<>();
        handlerRegistry.put("event", contentHandler);
        
        translationProcessor = new TranslationProcessor(
                translationJobRepository,
                languageRepository,
                translationPort,
                maskingService,
                handlerRegistry
        );
        
        // Set maxBackoffMinutes via reflection
        ReflectionTestUtils.setField(translationProcessor, "maxBackoffMinutes", 60);
    }

    @Test
    void testTranslationExistingInReviewedState() throws Exception {
        // Given
        TranslationJob job = createMockJob(1L, "event", 100L, 1L, 2L);
        Language sourceLanguage = createMockLanguage(1L, "es");
        Language targetLanguage = createMockLanguage(2L, "en");
        
        when(translationJobRepository.findByEntityAndStatus("event", 100L, TranslationJobStatus.PENDING))
                .thenReturn(List.of(job));
        when(translationJobRepository.claimJob(1L)).thenReturn(true);
        when(languageRepository.findById(1L)).thenReturn(Optional.of(sourceLanguage));
        when(languageRepository.findById(2L)).thenReturn(Optional.of(targetLanguage));
        when(contentHandler.findTranslationStatus(100L, "en"))
                .thenReturn(Optional.of(TranslationStatus.REVIEWED));

        // When
        translationProcessor.processTranslationJobs("event", 100L);

        // Then
        verify(translationJobRepository).claimJob(1L);
        verify(contentHandler).findTranslationStatus(100L, "en");
        verify(contentHandler, never()).saveTranslation(anyLong(), anyString(), anyMap(), any(), any());
        verify(translationPort, never()).translate(anyString(), anyString(), anyString());
        verify(job).markCompleted();
        verify(translationJobRepository).save(job);
    }

    @Test
    void testAtomicClaimReturnsZeroRows() throws Exception {
        // Given
        TranslationJob job = createMockJob(1L, "event", 100L, 1L, 2L);
        
        when(translationJobRepository.findByEntityAndStatus("event", 100L, TranslationJobStatus.PENDING))
                .thenReturn(List.of(job));
        when(translationJobRepository.claimJob(1L)).thenReturn(false); // Claim failed

        // When
        translationProcessor.processTranslationJobs("event", 100L);

        // Then
        verify(translationJobRepository).claimJob(1L);
        verify(languageRepository, never()).findById(anyLong());
        verify(contentHandler, never()).loadSourceTexts(anyLong());
        verify(translationPort, never()).translate(anyString(), anyString(), anyString());
        verify(contentHandler, never()).saveTranslation(anyLong(), anyString(), anyMap(), any(), any());
        verify(translationJobRepository, never()).save(any(TranslationJob.class));
    }

    @Test
    void testMissingPlaceholderAfterTranslation() throws Exception {
        // Given
        TranslationJob job = createMockJob(1L, "event", 100L, 1L, 2L);
        Language sourceLanguage = createMockLanguage(1L, "es");
        Language targetLanguage = createMockLanguage(2L, "en");
        
        Map<String, String> sourceTexts = new HashMap<>();
        sourceTexts.put("title", "Welcome to Tu Evento, {name}!");
        
        TextMaskingService.MaskingResult maskingResult = new TextMaskingService.MaskingResult(
                "Welcome to {{T_1}}, {{V_1}}!",
                Map.of("{{T_1}}", "Tu Evento"),
                Map.of("{{V_1}}", "{name}")
        );

        when(translationJobRepository.findByEntityAndStatus("event", 100L, TranslationJobStatus.PENDING))
                .thenReturn(List.of(job));
        when(translationJobRepository.claimJob(1L)).thenReturn(true);
        when(languageRepository.findById(1L)).thenReturn(Optional.of(sourceLanguage));
        when(languageRepository.findById(2L)).thenReturn(Optional.of(targetLanguage));
        when(contentHandler.findTranslationStatus(100L, "en"))
                .thenReturn(Optional.empty());
        when(contentHandler.loadSourceTexts(100L)).thenReturn(sourceTexts);
        when(maskingService.maskText("Welcome to Tu Evento, {name}!"))
                .thenReturn(maskingResult);
        when(translationPort.translate("Welcome to {{T_1}}, {{V_1}}!", "es", "en"))
                .thenReturn("Bienvenido"); // Missing placeholders
        when(maskingService.unmaskText(eq("Bienvenido"), anyMap(), anyMap()))
                .thenThrow(new TextMaskingService.MaskingException("Missing placeholder in translated text: {{T_1}}"));

        // When
        translationProcessor.processTranslationJobs("event", 100L);

        // Then
        verify(translationJobRepository).claimJob(1L);
        verify(contentHandler).loadSourceTexts(100L);
        verify(translationPort).translate("Welcome to {{T_1}}, {{V_1}}!", "es", "en");
        verify(maskingService).unmaskText("Bienvenido", 
                Map.of("{{T_1}}", "Tu Evento"), 
                Map.of("{{V_1}}", "{name}"));
        verify(contentHandler, never()).saveTranslation(anyLong(), anyString(), anyMap(), any(), any());
        verify(job).markFailed(contains("{{T_1}}"), eq(60));
        verify(translationJobRepository).save(job);
    }

    private TranslationJob createMockJob(Long jobId, String entityType, Long entityId, 
                                       Long sourceLanguageId, Long targetLanguageId) {
        TranslationJob job = mock(TranslationJob.class);
        when(job.getJobId()).thenReturn(jobId);
        when(job.getEntityType()).thenReturn(entityType);
        when(job.getEntityId()).thenReturn(entityId);
        when(job.getSourceLanguageId()).thenReturn(sourceLanguageId);
        when(job.getTargetLanguageId()).thenReturn(targetLanguageId);
        return job;
    }

    private Language createMockLanguage(Long id, String code) {
        Language language = mock(Language.class);
        when(language.getLanguageId()).thenReturn(id);
        when(language.getCode()).thenReturn(code);
        return language;
    }
}