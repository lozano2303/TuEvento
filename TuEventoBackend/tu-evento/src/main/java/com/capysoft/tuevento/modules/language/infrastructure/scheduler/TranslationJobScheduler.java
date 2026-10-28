package com.capysoft.tuevento.modules.language.infrastructure.scheduler;

import com.capysoft.tuevento.modules.language.application.service.TranslationProcessor;
import com.capysoft.tuevento.modules.language.domain.model.TranslationJob;
import com.capysoft.tuevento.modules.language.domain.repository.TranslationJobRepository;
import com.capysoft.tuevento.shared.domain.valueobject.TranslationJobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Scheduler que procesa automáticamente los jobs de traducción PENDING.
 * 
 * Se ejecuta en dos momentos:
 *   1. Al arrancar la aplicación (catch-up) — procesa jobs que quedaron PENDING
 *      si el backend estuvo caído o si el executor estaba lleno.
 *   2. Cada 15 segundos (cron) — caso normal de operación para procesar nuevos jobs.
 * 
 * El scheduler agrupa los jobs PENDING por entityType+entityId y despacha el
 * procesamiento asíncrono para cada entidad. Esto permite que múltiples idiomas
 * de la misma entidad se procesen en paralelo de forma eficiente.
 * 
 * @EnableScheduling está activado en TuEventoApplication.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "translation.mode", havingValue = "local", matchIfMissing = true)
public class TranslationJobScheduler {

    private final TranslationJobRepository translationJobRepository;
    private final TranslationProcessor translationProcessor;

    /**
     * Catch-up al arrancar: procesa jobs PENDING que pudieron haber quedado
     * sin procesar (backend reiniciado, executor lleno, etc).
     * 
     * ApplicationReadyEvent garantiza que el contexto de Spring está completamente
     * inicializado (DB, beans, transacciones).
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("[TranslationScheduler] Running startup catch-up for pending translation jobs...");
        processPendingJobs();
        log.info("[TranslationScheduler] Startup catch-up finished.");
    }

    /**
     * Cron cada 15 segundos - procesa jobs PENDING de forma automática.
     * 
     * Frecuencia elegida para balance entre:
     *  - Respuesta rápida para lazy loading (usuario espera traducción)
     *  - Evitar queries innecesarias cuando no hay jobs pendientes
     *  - Dar tiempo a que LibreTranslate complete traducciones (~17s cada una)
     */
    @Scheduled(cron = "*/15 * * * * *") // cada 15 segundos
    public void scheduledProcessing() {
        processPendingJobs();
    }

    private void processPendingJobs() {
        // 1. Buscar todos los jobs PENDING
        List<TranslationJob> pendingJobs = translationJobRepository.findByStatus(TranslationJobStatus.PENDING);

        if (pendingJobs.isEmpty()) {
            log.debug("[TranslationScheduler] No pending translation jobs to process.");
            return;
        }

        // 2. Agrupar por entityType+entityId
        Map<String, List<TranslationJob>> jobsByEntity = pendingJobs.stream()
                .collect(Collectors.groupingBy(job -> 
                        job.getEntityType() + ":" + job.getEntityId()));

        log.info("[TranslationScheduler] Found {} pending jobs across {} entities. Dispatching...",
                pendingJobs.size(), jobsByEntity.size());

        // 3. Despachar procesamiento asíncrono para cada entidad
        jobsByEntity.forEach((entityKey, jobs) -> {
            TranslationJob firstJob = jobs.get(0);
            try {
                translationProcessor.processTranslationJobs(
                        firstJob.getEntityType(), 
                        firstJob.getEntityId().longValue());
            } catch (Exception e) {
                log.error("[TranslationScheduler] Error dispatching jobs for {}: {}", 
                        entityKey, e.getMessage());
            }
        });
    }
}
