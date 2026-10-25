# ADR-003 — Webhooks firmados con HMAC-SHA256

**Estado:** Aceptado · **Fecha:** 2026

## Contexto

El endpoint de webhooks del sistema cliente es público por naturaleza. Sin autenticación, cualquiera podría marcar órdenes como pagadas. Las pasarelas comerciales resuelven esto firmando cada notificación.

## Decisión

- Cada webhook lleva la cabecera `X-Signature` con el HMAC-SHA256, en hexadecimal minúsculo, del cuerpo JSON exacto, usando un secreto compartido (`WEBHOOK_SECRET`).
- Cada transición genera un `eventId` UUID único que el cliente usa para idempotencia.
- Se hacen hasta 3 intentos de entrega con espera entre ellos, y cada intento se registra en `webhook_delivery_logs`.
- El primer intento es síncrono para que, si falla la persistencia del pago, no se notifique un estado que no existe.

## Alternativas consideradas

- **Sin firma:** descartada; no prepara al cliente para producción.
- **mTLS:** más fuerte pero desproporcionado para un sandbox y complejo en local.
- **Firma asimétrica (RSA/Ed25519):** evita compartir secretos, pero ninguna de las pasarelas de referencia del mercado local la exige; se reconsiderará en multi-comercio.

## Consecuencias

- ➕ El cliente implementa la misma verificación que necesitará con un proveedor real.
- ➕ Auditoría completa de intentos.
- ➖ El secreto debe distribuirse y rotarse fuera de banda.
- ➖ Sin *timestamp* firmado, la protección anti-*replay* recae en la idempotencia del cliente (L-10).
- ➕ Los reintentos corren en un bean `@Async` separado (`WebhookRetryScheduler`), sin bloquear la petición (L-01 resuelto).
- ➖ Los reintentos pendientes viven en memoria y se pierden si CPS se reinicia (L-13).
