package com.capysoft.tuevento.shared.infrastructure.messaging;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * Idempotencia de los consumidores de Kafka: recuerda qué {@code eventId} procesó cada consumidor.
 *
 * <p>Uso: si {@link #isProcessed} es true, descartar el mensaje; si no, procesarlo y al terminar
 * con éxito llamar a {@link #markProcessed}. Si el proceso muere entre ambos pasos el mensaje se
 * reprocesa, así que el efecto también debe tolerar repetirse (p. ej. no cancelar un evento ya
 * cancelado).
 */
@Component
@RequiredArgsConstructor
public class ProcessedEventStore {

    private final ProcessedEventJpaRepository repository;

    @Transactional(readOnly = true)
    public boolean isProcessed(String consumer, UUID eventId) {
        return repository.existsById(new ProcessedEventEntity.Key(consumer, eventId));
    }

    /** Idempotente: marcar dos veces el mismo mensaje no falla. */
    @Transactional
    public void markProcessed(String consumer, UUID eventId) {
        if (!isProcessed(consumer, eventId)) {
            repository.save(new ProcessedEventEntity(consumer, eventId, LocalDateTime.now()));
        }
    }
}
