package com.capysoft.tuevento.modules.language.domain.model;

import com.capysoft.tuevento.shared.domain.valueobject.TranslationJobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Representa un job de traducción en el dominio.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationJob {

    private Integer jobId;
    private String entityType;
    private Integer entityId;
    private Integer sourceLanguageId;
    private Integer targetLanguageId;
    private String sourceHash;
    private TranslationJobStatus status;
    private String provider;
    private Integer attempts;
    private String lastError;
    private LocalDateTime nextRetryAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    /**
     * Incrementa el número de intentos y calcula el próximo reintento.
     */
    public void incrementAttempts(int maxBackoffMinutes) {
        this.attempts = (this.attempts == null) ? 1 : this.attempts + 1;
        
        // Backoff: 2^attempts minutos, con máximo configurable
        long backoffMinutes = Math.min((long) Math.pow(2, this.attempts), maxBackoffMinutes);
        this.nextRetryAt = LocalDateTime.now().plusMinutes(backoffMinutes);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Marca el job como completado.
     */
    public void markCompleted() {
        this.status = TranslationJobStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.lastError = null;
    }

    /**
     * Marca el job como fallido con un error.
     */
    public void markFailed(String error, int maxBackoffMinutes) {
        this.status = TranslationJobStatus.FAILED;
        this.lastError = error != null && error.length() > 1000 ? error.substring(0, 1000) : error;
        incrementAttempts(maxBackoffMinutes);
    }

    /**
     * Marca el job como en proceso.
     */
    public void markProcessing() {
        this.status = TranslationJobStatus.PROCESSING;
        this.updatedAt = LocalDateTime.now();
    }
}