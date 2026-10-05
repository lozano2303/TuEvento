package com.capysoft.tuevento.modules.language.domain.model;

import com.capysoft.tuevento.shared.domain.valueobject.TranslationJobStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TranslationJobTest {

    @Test
    void testIncrementAttempts() {
        TranslationJob job = TranslationJob.builder()
                .attempts(0)
                .build();

        job.incrementAttempts(60);

        assertEquals(1, job.getAttempts());
        assertNotNull(job.getNextRetryAt());
        assertNotNull(job.getUpdatedAt());
        
        // First attempt should retry in 2 minutes (2^1)
        assertTrue(job.getNextRetryAt().isAfter(LocalDateTime.now().plusMinutes(1)));
        assertTrue(job.getNextRetryAt().isBefore(LocalDateTime.now().plusMinutes(3)));
    }

    @Test
    void testIncrementAttemptsWithBackoffLimit() {
        TranslationJob job = TranslationJob.builder()
                .attempts(10) // High number of attempts
                .build();

        job.incrementAttempts(30); // Max backoff 30 minutes

        assertEquals(11, job.getAttempts());
        
        // Should be limited to maxBackoffMinutes
        assertTrue(job.getNextRetryAt().isAfter(LocalDateTime.now().plusMinutes(29)));
        assertTrue(job.getNextRetryAt().isBefore(LocalDateTime.now().plusMinutes(31)));
    }

    @Test
    void testMarkCompleted() {
        TranslationJob job = TranslationJob.builder()
                .status(TranslationJobStatus.PROCESSING)
                .lastError("Some error")
                .build();

        job.markCompleted();

        assertEquals(TranslationJobStatus.COMPLETED, job.getStatus());
        assertNotNull(job.getCompletedAt());
        assertNotNull(job.getUpdatedAt());
        assertNull(job.getLastError());
    }

    @Test
    void testMarkFailed() {
        TranslationJob job = TranslationJob.builder()
                .status(TranslationJobStatus.PROCESSING)
                .attempts(1)
                .build();

        String errorMessage = "Translation service unavailable";
        job.markFailed(errorMessage, 60);

        assertEquals(TranslationJobStatus.FAILED, job.getStatus());
        assertEquals(errorMessage, job.getLastError());
        assertEquals(2, job.getAttempts()); // Incremented
        assertNotNull(job.getNextRetryAt());
        assertNotNull(job.getUpdatedAt());
    }

    @Test
    void testMarkFailedWithLongError() {
        TranslationJob job = TranslationJob.builder().build();
        
        // Create error message longer than 1000 characters
        String longError = "Error: " + "x".repeat(1000);
        job.markFailed(longError, 60);

        // Should truncate to 1000 characters
        assertEquals(1000, job.getLastError().length());
        assertTrue(job.getLastError().startsWith("Error:"));
    }

    @Test
    void testMarkProcessing() {
        TranslationJob job = TranslationJob.builder()
                .status(TranslationJobStatus.PENDING)
                .build();

        job.markProcessing();

        assertEquals(TranslationJobStatus.PROCESSING, job.getStatus());
        assertNotNull(job.getUpdatedAt());
    }

    @Test
    void testBackoffCalculation() {
        TranslationJob job = TranslationJob.builder().build();

        // Test exponential backoff: 2^attempts minutes
        job.setAttempts(1);
        job.incrementAttempts(120);
        assertEquals(2, job.getAttempts());
        // Next retry should be around 4 minutes from now (2^2)
        
        job.incrementAttempts(120);
        assertEquals(3, job.getAttempts());
        // Next retry should be around 8 minutes from now (2^3)
        
        job.incrementAttempts(120);
        assertEquals(4, job.getAttempts());
        // Next retry should be around 16 minutes from now (2^4)
    }
}