package com.capysoft.tuevento.modules.language.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuración del módulo de traducción.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class TranslationConfiguration {

    @Value("${translation.executor.core-pool-size:2}")
    private int corePoolSize;
    
    @Value("${translation.executor.max-pool-size:4}")
    private int maxPoolSize;
    
    @Value("${translation.executor.queue-capacity:50}")
    private int queueCapacity;

    /**
     * Executor dedicado para traducciones con política AbortPolicy.
     */
    @Bean("translationExecutor")
    public Executor translationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("translation-");
        
        // AbortPolicy: rechaza la tarea si no hay hilos disponibles
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        
        executor.initialize();
        return executor;
    }
}