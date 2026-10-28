package com.capysoft.tuevento.shared.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

import lombok.extern.slf4j.Slf4j;

/**
 * Contenedor de los consumidores de eventos de dominio que publica el outbox
 * ({@code tuevento.<agregado>}).
 *
 * <p>Si el consumidor lanza una excepción, el mensaje se reintenta con backoff exponencial
 * (1 s, 2 s, 4 s… hasta 1 min entre intentos) durante {@code messaging.consumers.max-retry-ms}
 * (30 min por defecto). Mientras tanto la partición espera: los mensajes posteriores del mismo
 * agregado no se adelantan. Agotado el plazo se registra un ERROR con el offset y se sigue; como
 * el consumidor es idempotente, ese mensaje se puede reprocesar rebobinando el grupo.
 *
 * <p>{@code messaging.consumers.enabled=false} lo desactiva junto con los listeners (los tests
 * corren sin Kafka).
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "messaging.consumers.enabled", havingValue = "true", matchIfMissing = true)
public class DomainEventsConsumerConfig {

    public static final String CONTAINER_FACTORY = "domainEventsListenerContainerFactory";

    @Bean(name = CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<String, String> domainEventsListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            @Value("${messaging.consumers.max-retry-ms:1800000}") long maxRetryMs) {
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxInterval(60_000L);
        backOff.setMaxElapsedTime(maxRetryMs);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler((rec, ex) ->
                log.error("Domain event gave up after retries: topic={}, partition={}, offset={}, key={}: {}",
                        rec.topic(), rec.partition(), rec.offset(), rec.key(), ex.getMessage()), backOff);

        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
