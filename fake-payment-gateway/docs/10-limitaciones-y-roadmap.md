# 10 — Limitaciones conocidas y hoja de ruta

**Producto:** CapySoft Payment Sandbox (CPS) 1.0.0 · **Versión del documento:** 1.1 · **Fecha:** 2026-10-24

Un producto se mide también por lo bien que conoce sus propios límites. Esta sección registra la deuda técnica identificada en la revisión de la 1.0.0, lo que ya se corrigió y el plan para el resto.

---

## 1. Limitaciones resueltas

| ID | Área | Problema original | Solución | Verificación |
|---|---|---|---|---|
| L-01 | Webhooks | `retryAsync` era `private` y se invocaba desde la misma clase, así que Spring no aplicaba `@Async`: los reintentos (2 s + 10 s) corrían en el hilo de la petición. | Reintentos movidos a `WebhookRetryScheduler`, un bean separado cuyo método público `@Async` se invoca a través del proxy de Spring. El envío y la firma están en `WebhookDeliveryClient`. Backoff configurable (`WEBHOOK_RETRY_BACKOFF_MS`). | `WebhookRetryAsyncTest` comprueba con el proxy real que la llamada vuelve de inmediato y que los intentos corren en otro hilo; `WebhookRetryTest` comprueba la cantidad de intentos. |
| L-02 | API | El manejador genérico capturaba los errores de validación, el JSON mal formado y los enums inválidos, y los devolvía como 500 con el mensaje interno. | Manejadores específicos: 400 `Validation failed` con detalle por campo, 400 `Malformed request`, 404, 405, 409 y 415. Los 500 devuelven un mensaje genérico y el detalle queda en el log. | `PaymentApiTest` (casos de validación, JSON inválido, enum inválido, 415, 404 y 500 sin filtrar detalles). |
| L-03 | Seguridad | `/admin/**` no tenía autenticación y el checkout dependía de esos endpoints. | `AdminApiKeyInterceptor` exige `X-Api-Key` en `/admin/**` con comparación en tiempo constante; CPS no arranca con la clave vacía. El checkout usa los nuevos endpoints `/public/checkout/{id}/{approve\|decline\|fail\|cancel}`; el reembolso solo existe en `/admin`. El backend de TuEvento envía la clave (`FAKE_GATEWAY_API_KEY`). | `PaymentApiTest` (401 sin clave o con clave incorrecta, 200 con clave válida, checkout sin clave, checkout sin reembolso, preflight CORS) y `AdminApiKeyInterceptorTest`. |
| L-04 | Webhooks | `RestTemplate` sin *timeouts*. | *Timeouts* de conexión (3 s) y de lectura (5 s), configurables. | Configuración en `WebConfig`. |
| L-09 | Calidad | No había pruebas automatizadas. | 53 pruebas en 9 clases: dominio, casos de uso, firma (vector RFC 4231), entrega, reintentos, filtro de seguridad y contrato HTTP. Se ejecutan con `./mvnw test` y no requieren base de datos. | `./mvnw test` → `Tests run: 53, Failures: 0`. |
| L-12 | Validación | La moneda no se validaba en el DTO; un valor de más de 3 caracteres fallaba en la base de datos con 500. | `@Pattern([A-Za-z]{3})` en `currency` y `@Size(max=100)` en `externalReference`. | `PaymentApiTest.crearPagoConCamposInvalidosDevuelve400ConDetallePorCampo`. |

## 2. Limitaciones abiertas

| ID | Área | Descripción | Impacto | Severidad | Versión objetivo |
|---|---|---|---|---|---|
| L-05 | Datos | Esquema gestionado con `ddl-auto: update`, sin migraciones versionadas; los cambios en `CHECK` no se aplican a bases existentes. | Riesgo de deriva de esquema | Baja | 1.2.0 |
| L-06 | API | `GET /admin/payments` sin paginación, filtros ni orden. | Lento con muchos registros | Baja | 1.2.0 |
| L-07 | Auditoría | `webhook_delivery_logs` no guarda `payment_id` ni el cuerpo enviado. | Trazabilidad por pago limitada | Baja | 1.2.0 |
| L-08 | Concurrencia | Sin bloqueo optimista (`@Version`) sobre `payments`. | Requiere una sola réplica | Baja | 1.2.0 |
| L-10 | Seguridad | La firma no incluye *timestamp*; la protección anti-*replay* depende de la idempotencia del cliente. | Bajo | Baja | 2.0.0 |
| L-11 | Dominio | `externalReference` no es único: reintentar una creación genera pagos duplicados. | Duplicados si el cliente reintenta | Baja | 1.2.0 |
| L-13 | Webhooks | Los webhooks se envían dentro de la transacción, antes del *commit*; si la transacción revierte, el cliente pudo recibir un evento de un estado no persistido. Los reintentos asíncronos tampoco sobreviven a un reinicio de CPS. | Inconsistencia transitoria | Baja | 2.0.0 (outbox) |
| L-14 | Pruebas | Las pruebas no cubren la integración con PostgreSQL real (restricciones `CHECK` y `UNIQUE`). | Cubierto hoy solo por pruebas manuales | Baja | 1.2.0 (Testcontainers) |

## 3. Hoja de ruta

### 1.1.0 — Robustez *(en curso: L-01, L-02, L-03, L-04, L-09 y L-12 ya resueltos)*

- Documentación OpenAPI generada con springdoc y Swagger UI.
- Ejecutar `./mvnw test` del gateway en el pipeline de CI.

### 1.2.0 — Operación

- Migraciones con Liquibase (L-05).
- Paginación y filtros en el listado (L-06).
- `payment_id`, cuerpo y latencia en la bitácora de webhooks; endpoint para consultarla y reenviar eventos (L-07).
- `@Version` en la entidad de pagos (L-08).
- Idempotencia de creación por `externalReference` o cabecera `Idempotency-Key` (L-11).
- Pruebas de integración con Testcontainers (L-14).
- *Health check* con Spring Boot Actuator para orquestadores.

### 2.0.0 — Plataforma

- Multi-comercio: varios sistemas cliente con credenciales, URL de webhook y secreto propios.
- Patrón *transactional outbox* para webhooks (L-13).
- Firma con *timestamp* (`X-Signature: t=…,v1=…`) y ventana de tolerancia (L-10).
- Escenarios automáticos configurables (p. ej. montos terminados en `.13` se rechazan) para pruebas sin intervención humana.
- Expiración automática de pagos `PENDING`.
- Reembolsos parciales.
- Panel administrativo web con búsqueda y reenvío de webhooks.

## 4. Historial de versiones

| Versión | Fecha | Resumen |
|---|---|---|
| 1.0.0 | 2026 | Ciclo de vida completo (crear, aprobar, rechazar, fallar, cancelar, reembolsar), checkout hospedado, webhooks firmados con reintentos y auditoría. |
| 1.0.0 + correcciones | 2026-10-24 | Reintentos realmente asíncronos y *timeouts*, contrato de errores 4xx, API key en `/admin` con checkout público dedicado y suite de 53 pruebas automatizadas. Detalle en [`CHANGELOG.md`](../CHANGELOG.md). |
