package com.capysoft.tuevento.modules.language.application.service;

import com.capysoft.tuevento.modules.language.domain.model.TranslationJob;
import com.capysoft.tuevento.modules.language.domain.repository.TranslationJobRepository;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationJobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler para reintentar jobs de traducción fallidos o con timeout.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TranslationRetryScheduler {

    private final TranslationJobRepository translationJobRepository;
    private final TranslationProcessor translationProcessor;
    
    @Value("${translation.max-attempts:3}")
    private int maxAttempts;
    
    @Value("${translation.processing-timeout-minutes:10}")
    private int processingTimeoutMinutes;

    /**
     * Reintenta jobs fallidos o pendientes cada 5 minutos.
     */
    @Scheduled(fixedDelay = 300000) // 5 minutos
    public void retryFailedJobs() {
        LocalDateTime now = LocalDateTime.now();
        
        // Buscar jobs para reintento (FAILED/PENDING con retry time pasado)
        List<TranslationJob> jobsToRetry = translationJobRepository.findJobsForRetry(now, maxAttempts);
        
        if (!jobsToRetry.isEmpty()) {
            log.info("Found {} jobs for retry", jobsToRetry.size());
            
            for (TranslationJob job : jobsToRetry) {
                try {
                    // Reset status to PENDING
                    job.setStatus(TranslationJobStatus.PENDING);
                    translationJobRepository.save(job);
                    
                    // Dispatch processing
                    translationProcessor.processTranslationJobs(job.getEntityType(), job.getEntityId());
                    
                } catch (TaskRejectedException e) {
                    log.warn("Executor full, job {} will retry later", job.getJobId());
                } catch (Exception e) {
                    log.error("Error dispatching retry for job {}: {}", job.getJobId(), e.getMessage());
                }
            }
        }
    }

    /**
     * Rescata jobs en PROCESSING con timeout cada 10 minutos.
     */
    @Scheduled(fixedDelay = 600000) // 10 minutos  
    public void rescueTimedOutJobs() {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(processingTimeoutMinutes);
        
        List<TranslationJob> timedOutJobs = translationJobRepository.findProcessingJobsWithTimeout(timeoutThreshold);
        
        if (!timedOutJobs.isEmpty()) {
            log.warn("Found {} jobs with processing timeout, marking as FAILED", timedOutJobs.size());
            
            for (TranslationJob job : timedOutJobs) {
                job.markFailed("Processing timeout after " + processingTimeoutMinutes + " minutes", 60);
                translationJobRepository.save(job);
            }
        }
    }
}