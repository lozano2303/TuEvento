package com.capysoft.tuevento.modules.language.application.service;

import com.capysoft.tuevento.modules.language.application.port.out.TranslatableContentHandler;
import com.capysoft.tuevento.modules.language.application.port.out.TranslationException;
import com.capysoft.tuevento.modules.language.application.port.out.TranslationPort;
import com.capysoft.tuevento.modules.language.domain.model.Language;
import com.capysoft.tuevento.modules.language.domain.model.TranslationJob;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.modules.language.domain.repository.TranslationJobRepository;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationJobStatus;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationSource;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Procesador asíncrono de jobs de traducción.
 */
@Component
@Slf4j
public class TranslationProcessor {

    private final TranslationJobRepository translationJobRepository;
    private final LanguageRepository languageRepository;
    private final TranslationPort translationPort;
    private final TextMaskingService maskingService;
    
    @Autowired
    @Qualifier("translatableContentHandlerRegistry")
    private Map<String, TranslatableContentHandler> handlerRegistry;
    
    @Value("${translation.backoff.max-minutes:60}")
    private int maxBackoffMinutes;

    public TranslationProcessor(TranslationJobRepository translationJobRepository,
                              LanguageRepository languageRepository,
                              TranslationPort translationPort,
                              TextMaskingService maskingService) {
        this.translationJobRepository = translationJobRepository;
        this.languageRepository = languageRepository;
        this.translationPort = translationPort;
        this.maskingService = maskingService;
    }

    /**
     * Procesa jobs de traducción de una entidad de forma asíncrona.
     */
    @Async("translationExecutor")
    public CompletableFuture<Void> processTranslationJobs(String entityType, Long entityId) {
        log.debug("Processing translation jobs for {}/{}", entityType, entityId);
        
        // Buscar jobs PENDING para esta entidad
        List<TranslationJob> pendingJobs = translationJobRepository
                .findByEntityAndStatus(entityType, entityId, TranslationJobStatus.PENDING);
                
        TranslatableContentHandler handler = handlerRegistry.get(entityType);
        if (handler == null) {
            log.error("No handler found for entity type: {}", entityType);
            return CompletableFuture.completedFuture(null);
        }
        
        for (TranslationJob job : pendingJobs) {
            processJob(job, handler);
        }
        
        log.debug("Completed processing {} jobs for {}/{}", pendingJobs.size(), entityType, entityId);
        return CompletableFuture.completedFuture(null);
    }

    private void processJob(TranslationJob job, TranslatableContentHandler handler) {
        // 1. Reclamar job atómicamente
        boolean claimed = translationJobRepository.claimJob(job.getJobId());
        if (!claimed) {
            log.debug("Job {} already being processed", job.getJobId());
            return;
        }

        try {
            // 2. Cargar idiomas
            Optional<Language> sourceLanguage = languageRepository.findById(job.getSourceLanguageId());
            Optional<Language> targetLanguage = languageRepository.findById(job.getTargetLanguageId());
            
            if (sourceLanguage.isEmpty() || targetLanguage.isEmpty()) {
                job.markFailed("Source or target language not found", maxBackoffMinutes);
                translationJobRepository.save(job);
                return;
            }

            // 3. Verificar si ya existe traducción REVIEWED
            Optional<TranslationStatus> existingStatus = handler.findTranslationStatus(
                    job.getEntityId(), targetLanguage.get().getCode());
                    
            if (existingStatus.isPresent() && existingStatus.get() == TranslationStatus.PUBLISHED) {
                log.info("Skipping translation save - content is PUBLISHED: {}/{}/{}", 
                    job.getEntityType(), job.getEntityId(), targetLanguage.get().getCode());
                job.markCompleted();
                translationJobRepository.save(job);
                return;
            }

            // 4. Cargar textos fuente
            Map<String, String> sourceTexts = handler.loadSourceTexts(job.getEntityId());
            if (sourceTexts.isEmpty()) {
                job.markCompleted(); // No hay nada que traducir
                translationJobRepository.save(job);
                return;
            }

            // 5. Traducir cada campo
            Map<String, String> translatedTexts = new HashMap<>();
            
            for (Map.Entry<String, String> entry : sourceTexts.entrySet()) {
                String fieldName = entry.getKey();
                String sourceText = entry.getValue();
                
                if (sourceText == null || sourceText.trim().isEmpty()) {
                    translatedTexts.put(fieldName, sourceText);
                    continue;
                }

                try {
                    String translatedText = translateText(sourceText, 
                            sourceLanguage.get().getCode(), targetLanguage.get().getCode());
                    translatedTexts.put(fieldName, translatedText);
                } catch (Exception e) {
                    log.error("Failed to translate field {} for job {}: {}", fieldName, job.getJobId(), e.getMessage());
                    job.markFailed("Translation failed for field " + fieldName + ": " + e.getMessage(), maxBackoffMinutes);
                    translationJobRepository.save(job);
                    return;
                }
            }

            // 6. Guardar traducción
            handler.saveTranslation(job.getEntityId(), targetLanguage.get().getCode(), 
                    translatedTexts, TranslationSource.MACHINE, TranslationStatus.DRAFT);
            
            // 7. Marcar job como completado
            job.markCompleted();
            translationJobRepository.save(job);
            
            log.info("Successfully completed translation job {} for {}/{} -> {}", 
                    job.getJobId(), job.getEntityType(), job.getEntityId(), targetLanguage.get().getCode());

        } catch (Exception e) {
            log.error("Unexpected error processing job {}: {}", job.getJobId(), e.getMessage(), e);
            job.markFailed("Unexpected error: " + e.getMessage(), maxBackoffMinutes);
            translationJobRepository.save(job);
        }
    }

    private String translateText(String sourceText, String sourceLang, String targetLang) 
            throws TranslationException, TextMaskingService.MaskingException {
        
        // 1. Enmascarar términos protegidos y variables
        TextMaskingService.MaskingResult maskingResult = maskingService.maskText(sourceText);
        
        // 2. Traducir texto enmascarado
        String translatedMaskedText = translationPort.translate(maskingResult.maskedText, sourceLang, targetLang);
        
        // 3. Restaurar marcadores
        return maskingService.unmaskText(translatedMaskedText, 
                maskingResult.termMappings, maskingResult.variableMappings);
    }
}