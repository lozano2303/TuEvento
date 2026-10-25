# ADR-002 — Máquina de estados encapsulada en el dominio

**Estado:** Aceptado · **Fecha:** 2026

## Contexto

Un sandbox que permite cualquier cambio de estado (p. ej. reembolsar un pago rechazado) no prepara al sistema cliente para el comportamiento de una pasarela real, que sí rechaza esas operaciones.

## Decisión

- `PaymentStatus` declara en cada valor del enum sus transiciones permitidas (`allowedTransitions()`).
- `Payment` solo cambia de estado a través de métodos de negocio (`approve`, `decline`, `fail`, `cancel`, `refund`, `markAsProcessing`) que llaman a `transitionTo`, el cual lanza `InvalidStatusTransitionException` si la transición no está permitida.
- Cada transición regenera `webhookEventId` y devuelve un evento de dominio `PaymentStatusChanged`.
- La base de datos refuerza la lista de estados con una restricción `CHECK`.
- Aprobar, rechazar y fallar pasan siempre por `PROCESSING`, como ocurre en una autorización real.

## Consecuencias

- ➕ Las reglas viven en un solo lugar y se pueden probar sin infraestructura.
- ➕ Los errores de integración del cliente (operaciones fuera de orden) aparecen en desarrollo como HTTP 400.
- ➕ Agregar un estado (como se hizo con `REFUNDED`) es local al enum y produce automáticamente un nuevo tipo de webhook.
- ➖ La restricción `CHECK` debe actualizarse a mano en bases existentes mientras no haya migraciones versionadas (L-05).
