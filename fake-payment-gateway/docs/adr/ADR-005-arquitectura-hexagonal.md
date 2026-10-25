# ADR-005 — Arquitectura hexagonal con puertos de salida

**Estado:** Aceptado · **Fecha:** 2026

## Contexto

CPS debe poder evolucionar (nuevos estados, otra estrategia de entrega de webhooks, otra base de datos, cola de mensajes) sin reescribir sus reglas de negocio. También debe ser coherente con la arquitectura N-capas + DDD del resto de la plataforma de CapySoft.

## Decisión

Organizar el código en cuatro paquetes con dependencias hacia adentro:

```
interfaces.rest  →  application  →  domain
infrastructure   →  application / domain
```

- `domain` no importa Spring ni JPA. Define `PaymentRepository` como interfaz.
- `application` define `WebhookNotifierPort` y un caso de uso por clase.
- `infrastructure` implementa los puertos (`PaymentRepositoryAdapter`, `WebhookNotifierAdapter`).
- `interfaces.rest` solo llama a casos de uso. *Excepción conocida:* `AdminPaymentController.listPayments` usa el repositorio directamente; se moverá a un `ListPaymentsUseCase` en la 1.2.0.

## Consecuencias

- ➕ El dominio se prueba con JUnit puro.
- ➕ Reemplazar la entrega HTTP por un *outbox* o una cola solo toca la infraestructura.
- ➕ Estructura familiar para cualquier desarrollador de CapySoft.
- ➖ Más clases y mapeo explícito entidad ↔ dominio.
