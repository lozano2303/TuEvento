package com.capysoft.tuevento.modules.language.application.service;

import com.capysoft.tuevento.modules.language.application.port.out.TranslatableContentHandler;
import com.capysoft.tuevento.modules.language.domain.model.Language;
import com.capysoft.tuevento.modules.language.domain.model.TranslationJob;
import com.capysoft.tuevento.modules.language.domain.repository.LanguageRepository;
import com.capysoft.tuevento.modules.language.domain.repository.TranslationJobRepository;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationJobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Servicio de traducción síncrono.
 * Persiste jobs PENDING y despacha procesamiento asíncrono.
 */
@Service
@Slf4j
public class TranslationService {

    private final TranslationJobRepository translationJobRepository;
    private final LanguageRepository languageRepository;
    private final TranslationProcessor translationProcessor;
    
    @Autowired
    @Qualifier("translatableContentHandlerRegistry")
    private Map<String, TranslatableContentHandler> handlerRegistry;
    
    @Value("${translation.provider:libretranslate}")
    private String defaultProvider;

    public TranslationService(TranslationJobRepository translationJobRepository,
                            LanguageRepository languageRepository,
                            TranslationProcessor translationProcessor) {
        this.translationJobRepository = translationJobRepository;
        this.languageRepository = languageRepository;
        this.translationProcessor = translationProcessor;
    }

    /**
     * Solicita la traducción de una entidad.
     */
    @Transactional
    public void requestTranslation(String entityType, Long entityId, String sourceLanguageCode) {
        // 1. Validar que existe un handler para la entidad
        TranslatableContentHandler handler = handlerRegistry.get(entityType);
        if (handler == null) {
            log.warn("No handler found for entity type: {}", entityType);
            return;
        }

        // 2. Cargar textos fuente y calcular hash
        Map<String, String> sourceTexts = handler.loadSourceTexts(entityId);
        if (sourceTexts.isEmpty()) {
            log.debug("No translatable texts found for {}/{}", entityType, entityId);
            return;
        }
        
        String sourceHash = calculateSourceHash(sourceTexts);
        
        // 3. Obtener idiomas
        Optional<Language> sourceLanguage = languageRepository.findByCode(sourceLanguageCode);
        if (sourceLanguage.isEmpty()) {
            log.warn("Source language not found: {}", sourceLanguageCode);
            return;
        }
        
        List<Language> targetLanguages = languageRepository.findAllActive()
                .stream()
                .filter(lang -> !lang.getCode().equals(sourceLanguageCode))
                .toList();

        // 4. Crear/actualizar jobs PENDING
        List<TranslationJob> jobsToSave = new ArrayList<>();
        
        for (Language targetLanguage : targetLanguages) {
            Optional<TranslationJob> existingJob = translationJobRepository
                    .findByEntityAndTargetLanguage(entityType, entityId.intValue(), targetLanguage.getLanguageId());
                    
            if (existingJob.isPresent()) {
                TranslationJob job = existingJob.get();
                // Solo actualizar si el hash cambió
                if (!sourceHash.equals(job.getSourceHash())) {
                    job.setSourceHash(sourceHash);
                    job.setStatus(TranslationJobStatus.PENDING);
                    job.setAttempts(0);
                    job.setLastError(null);
                    job.setNextRetryAt(null);
                    job.setUpdatedAt(LocalDateTime.now());
                    jobsToSave.add(job);
                    
                    log.debug("Updated translation job for {}/{} -> {}", entityType, entityId, targetLanguage.getCode());
                }
            } else {
                // Crear nuevo job
                TranslationJob newJob = TranslationJob.builder()
                        .entityType(entityType)
                        .entityId(entityId.intValue())
                        .sourceLanguageId(sourceLanguage.get().getLanguageId())
                        .targetLanguageId(targetLanguage.getLanguageId())
                        .sourceHash(sourceHash)
                        .status(TranslationJobStatus.PENDING)
                        .provider(defaultProvider)
                        .attempts(0)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                jobsToSave.add(newJob);
                
                log.debug("Created translation job for {}/{} -> {}", entityType, entityId, targetLanguage.getCode());
            }
        }

        if (!jobsToSave.isEmpty()) {
            translationJobRepository.saveAll(jobsToSave);
            log.info("Saved {} translation jobs for {}/{}", jobsToSave.size(), entityType, entityId);
            
            // 5. Despachar procesamiento después del commit
            scheduleProcessingAfterCommit(entityType, entityId);
        }
    }

    private void scheduleProcessingAfterCommit(String entityType, Long entityId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatchProcessing(entityType, entityId);
                }
            });
        } else {
            // No hay transacción activa, despachar directamente
            dispatchProcessing(entityType, entityId);
        }
    }

    private void dispatchProcessing(String entityType, Long entityId) {
        try {
            translationProcessor.processTranslationJobs(entityType, entityId);
        } catch (TaskRejectedException e) {
            log.warn("Translation executor full, jobs remain PENDING for retry: {}/{} - {}", 
                    entityType, entityId, e.getMessage());
            // Jobs quedan PENDING, el scheduler los recogerá
        }
    }

    private String calculateSourceHash(Map<String, String> sourceTexts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            // Concatenar todos los textos de forma determinística (ordenando por clave)
            StringBuilder sb = new StringBuilder();
            sourceTexts.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> sb.append(entry.getKey()).append("=").append(entry.getValue()).append(";"));
            
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            
            // Convertir a hex
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}