package com.capysoft.tuevento.shared.infrastructure.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuración del productor Kafka para el outbox relay.
 *
 * <p>Usa {@code acks=all} + {@code enable.idempotence=true} para garantizar
 * que cada mensaje se escribe exactamente una vez en el broker incluso ante
 * reintentos por timeout de red. El relay se encarga del at-least-once a nivel
 * de aplicación; la idempotencia del productor evita duplicados en Kafka por
 * reintentos internos del cliente.
 *
 * <p>El serializer es {@code StringSerializer} porque el outbox ya persiste el
 * payload como JSON. No se usa Avro ni Schema Registry para mantener la
 * independencia de esquema durante la migración.
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, String> outboxProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // Garantías de durabilidad: el broker confirma que todas las réplicas ISR
        // escribieron el mensaje antes de responder al productor.
        props.put(ProducerConfig.ACKS_CONFIG, "all");

        // Idempotencia del productor: evita duplicados en Kafka por reintentos de red.
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // Reintentos internos (el relay también reintenta, pero esto cubre flaps de red cortos).
        props.put(ProducerConfig.RETRIES_CONFIG, 3);

        // Agrupa mensajes en lotes de hasta 16 KB para reducir overhead de red.
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);

        // Espera hasta 5 ms para agrupar mensajes en el mismo batch.
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(outboxProducerFactory());
    }
}
