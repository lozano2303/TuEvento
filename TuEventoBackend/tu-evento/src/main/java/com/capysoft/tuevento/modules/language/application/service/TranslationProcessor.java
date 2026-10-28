package com.capysoft.tuevento.modules.language.application.service;

import com.capysoft.tuevento.modules.language.application.port.out.TranslationException;
import com.capysoft.tuevento.modules.language.application.port.out.TranslationPort;
import com.capysoft.tuevento.modules.language.application.port.out.TranslationResults;
import com.capysoft.tuevento.modules.language.domain.model.Language;
import com.capysoft.tuevento.modules.language.domain.model.TranslationJob;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.modules.language.domain.repository.TranslationJobRepository;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationJobStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@Slf4j
@Component
@ConditionalOnProperty(name = "translation.mode", havingValue = "local", matchIfMissing = true)
public class TranslationProcessor {

    private final TranslationJobRepository translationJobRepository;
    private final LanguageRepository languageRepository;
    private final TranslationPort translationPort;
    private final TextMaskingService maskingService;
    private final TranslationResults translationResults;

    @Value("${translation.backoff.max-minutes:60}")
    private int maxBackoffMinutes;

    public TranslationProcessor(TranslationJobRepository translationJobRepository,
                              LanguageRepository languageRepository,
                              TranslationPort translationPort,
                              TextMaskingService maskingService,
                              TranslationResults translationResults) {
        this.translationJobRepository = translationJobRepository;
        this.languageRepository = languageRepository;
        this.translationPort = translationPort;
        this.maskingService = maskingService;
        this.translationResults = translationResults;
    }

    /**
     * Procesa jobs de traducción de una entidad de forma asíncrona.
     */
    @Async("translationExecutor")
    public CompletableFuture<Void> processTranslationJobs(String entityType, Long entityId) {
        try {
            log.debug("Processing translation jobs for {}/{}", entityType, entityId);
            
            // Buscar jobs PENDING para esta entidad
            List<TranslationJob> pendingJobs = translationJobRepository
                    .findByEntityAndStatus(entityType, entityId.intValue(), TranslationJobStatus.PENDING);
            
            log.info("Found {} pending jobs for {}/{}", pendingJobs.size(), entityType, entityId);

            // Procesar jobs en paralelo para mejorar rendimiento
            pendingJobs.parallelStream().forEach(job -> {
                try {
                    processJob(job);
                } catch (Exception e) {
                    log.error("Error processing job {} in parallel: {}", job.getJobId(), e.getMessage(), e);
                }
            });
            
            log.debug("Completed processing {} jobs for {}/{}", pendingJobs.size(), entityType, entityId);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("CRITICAL ERROR in processTranslationJobs for {}/{}: {}", entityType, entityId, e.getMessage(), e);
            throw e;
        }
    }

    // Sin @Transactional a propósito: se invoca desde la propia clase (sin proxy) y una
    // transacción abierta durante la llamada a LibreTranslate retendría conexiones. Cada
    // operación del repositorio (claimJob atómico, save) es transaccional por sí misma.
    private void processJob(TranslationJob job) {
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

            // 3. Textos fuente guardados al pedir la traducción
            Map<String, String> sourceTexts = job.getSourceTexts();
            if (sourceTexts == null || sourceTexts.isEmpty()) {
                job.markCompleted(); // No hay nada que traducir
                translationJobRepository.save(job);
                return;
            }

            // 4. Traducir cada campo
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

            // 5. Entregar la traducción al dominio (la guarda publicada, sin pisar una ya publicada)
            translationResults.deliver(job.getEntityType(), job.getEntityId().longValue(),
                    targetLanguage.get().getCode(), translatedTexts);

            // 6. Marcar job como completado
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
        try {
            return maskingService.unmaskText(translatedMaskedText,
                    maskingResult.termMappings, maskingResult.variableMappings);
        } catch (TextMaskingService.MaskingException e) {
            // 4. Respaldo: el motor a veces elimina el marcador (p. ej. al inicio de la frase).
            //    Se traduce el texto sin enmascarar para no dejar el job fallando indefinidamente;
            //    solo se acepta si las variables siguen intactas (una variable rota es peor).
            log.warn("Placeholder lost translating {}->{} ({}); retrying without masking",
                    sourceLang, targetLang, e.getMessage());
            String plain = translationPort.translate(sourceText, sourceLang, targetLang);
            boolean variablesKept = plain != null
                    && maskingResult.variableMappings.values().stream().allMatch(plain::contains);
            if (!variablesKept) {
                throw e;
            }
            return plain;
        }
    }
}