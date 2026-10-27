package com.capysoft.tuevento.modules.language.application.service;

import com.capysoft.tuevento.modules.language.domain.model.Language;
import com.capysoft.tuevento.modules.language.domain.model.TranslationJob;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.modules.language.domain.repository.TranslationJobRepository;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationJobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TranslationServiceTest {

    @Mock
    private TranslationJobRepository translationJobRepository;
    
    @Mock
    private LanguageRepository languageRepository;
    
    @Mock
    private TranslationProcessor translationProcessor;
    

    private TranslationService translationService;

    @BeforeEach
    void setUp() {
        translationService = new TranslationService(
                translationJobRepository,
                languageRepository,
                translationProcessor
        );
        
        
        ReflectionTestUtils.setField(translationService, "defaultProvider", "libretranslate");
    }

    @Test
    void testRequestTranslationSuccess() {
        // Arrange
        String entityType = "test_entity";
        Long entityId = 1L;
        String sourceLanguageCode = "es";
        
        Map<String, String> sourceTexts = Map.of("title", "Test Title", "description", "Test Description");
        
        Language sourceLanguage = Language.builder()
                .languageId(1)
                .code("es")
                .name("Español")
                .build();
                
        Language targetLanguage = Language.builder()
                .languageId(2)
                .code("en")
                .name("English")
                .build();
        
        when(languageRepository.findByCode(sourceLanguageCode)).thenReturn(Optional.of(sourceLanguage));
        when(languageRepository.findAllActive()).thenReturn(List.of(sourceLanguage, targetLanguage));
        when(translationJobRepository.findByEntityAndTargetLanguage(eq(entityType), eq(1), eq(2)))
                .thenReturn(Optional.empty());
        when(translationJobRepository.saveAll(anyList())).thenReturn(List.of());

        // Act
        translationService.requestTranslation(entityType, entityId, sourceLanguageCode, sourceTexts);

        // Assert
        verify(translationJobRepository).saveAll(argThat(jobs -> {
            List<TranslationJob> jobList = (List<TranslationJob>) jobs;
            return jobList.size() == 1 && 
                   jobList.get(0).getEntityType().equals(entityType) &&
                   jobList.get(0).getEntityId().equals(entityId.intValue()) && // entityId del job es Integer
                   jobList.get(0).getStatus() == TranslationJobStatus.PENDING &&
                   sourceTexts.equals(jobList.get(0).getSourceTexts());
        }));
    }

    @Test
    void testRequestTranslationWithExistingJob() {
        // Arrange
        String entityType = "test_entity";
        Long entityId = 1L;
        String sourceLanguageCode = "es";
        
        Map<String, String> sourceTexts = Map.of("title", "Updated Title");
        String newHash = "new_hash";
        
        Language sourceLanguage = Language.builder()
                .languageId(1)
                .code("es")
                .build();
                
        Language targetLanguage = Language.builder()
                .languageId(2)
                .code("en")
                .build();
        
        TranslationJob existingJob = TranslationJob.builder()
                .jobId(1)
                .entityType(entityType)
                .entityId(entityId.intValue())
                .sourceHash("old_hash") // Different hash
                .status(TranslationJobStatus.COMPLETED)
                .build();
        
        when(languageRepository.findByCode(sourceLanguageCode)).thenReturn(Optional.of(sourceLanguage));
        when(languageRepository.findAllActive()).thenReturn(List.of(sourceLanguage, targetLanguage));
        when(translationJobRepository.findByEntityAndTargetLanguage(eq(entityType), eq(1), eq(2)))
                .thenReturn(Optional.of(existingJob));
        when(translationJobRepository.saveAll(anyList())).thenReturn(List.of());

        // Act
        translationService.requestTranslation(entityType, entityId, sourceLanguageCode, sourceTexts);

        // Assert
        verify(translationJobRepository).saveAll(argThat(jobs -> {
            List<TranslationJob> jobList = (List<TranslationJob>) jobs;
            return jobList.size() == 1 && 
                   jobList.get(0).getStatus() == TranslationJobStatus.PENDING &&
                   jobList.get(0).getAttempts() == 0; // Reset attempts
        }));
    }

    @Test
    void testRequestTranslationUnknownSourceLanguage() {
        // Arrange
        when(languageRepository.findByCode("xx")).thenReturn(Optional.empty());

        // Act
        translationService.requestTranslation("test_entity", 1L, "xx", Map.of("title", "Hola"));

        // Assert
        verify(translationJobRepository, never()).saveAll(any());
    }

    @Test
    void testRequestTranslationEmptyTexts() {
        // Arrange
        String entityType = "test_entity";
        Long entityId = 1L;
        String sourceLanguageCode = "es";

        // Act
        translationService.requestTranslation(entityType, entityId, sourceLanguageCode, Map.of());

        // Assert
        verify(translationJobRepository, never()).saveAll(any());
    }

    @Test
    void testRequestTranslationWithTaskRejected() {
        // Arrange
        String entityType = "test_entity";
        Long entityId = 1L;
        String sourceLanguageCode = "es";
        
        Map<String, String> sourceTexts = Map.of("title", "Test Title");
        
        Language sourceLanguage = Language.builder()
                .languageId(1)
                .code("es")
                .build();
                
        Language targetLanguage = Language.builder()
                .languageId(2)
                .code("en")
                .build();
        
        when(languageRepository.findByCode(sourceLanguageCode)).thenReturn(Optional.of(sourceLanguage));
        when(languageRepository.findAllActive()).thenReturn(List.of(sourceLanguage, targetLanguage));
        when(translationJobRepository.findByEntityAndTargetLanguage(eq(entityType), eq(1), eq(2)))
                .thenReturn(Optional.empty());
        when(translationJobRepository.saveAll(anyList())).thenReturn(List.of());
        
        // Mock TaskRejectedException
        doThrow(new TaskRejectedException("Executor full"))
                .when(translationProcessor).processTranslationJobs(anyString(), anyLong());

        // Act & Assert - should not throw exception
        translationService.requestTranslation(entityType, entityId, sourceLanguageCode, sourceTexts);
        
        verify(translationJobRepository).saveAll(any()); // Jobs should still be saved
    }
}
