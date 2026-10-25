# 08 — Plan de pruebas

**Producto:** CapySoft Payment Sandbox (CPS) 1.0.0 · **Versión del documento:** 1.0 · **Fecha:** 2026-10-24
**Estructura de referencia:** ISO/IEC/IEEE 29119-3 (simplificada)

---

## 1. Objetivo

Verificar que CPS cumple los requisitos del [SRS](01-SRS.md) y que un sistema cliente puede integrarse de forma fiable.

## 2. Estado actual

CPS tiene **52 pruebas automatizadas** en 9 clases, que se ejecutan con `./mvnw test` en unos 15 s y **no requieren base de datos ni red**. Se complementan con pruebas manuales extremo a extremo a través de la plataforma TuEvento (sección 6). La integración con PostgreSQL real todavía se verifica de forma manual (L-14).

```text
./mvnw test
Tests run: 52, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
```

| Clase de prueba | Pruebas | Qué verifica |
|---|---|---|
| `domain.model.PaymentStatusTest` | 3 | La tabla completa de transiciones (7 × 7) coincide con RF-08; solo `APPROVED` se reembolsa; estados terminales |
| `domain.model.PaymentTest` | 6 | Creación, `eventId` nuevo por transición, contenido del evento, flujo aprobar y reembolsar, transición inválida sin efectos secundarios |
| `domain.model.MoneyTest` | 4 | Normalización de moneda, montos negativos o nulos, moneda vacía, igualdad sin importar la escala |
| `application.usecase.PaymentUseCasesTest` | 9 | Cada caso de uso: persistencia, webhooks emitidos y en qué orden, reembolso inválido sin persistir ni notificar, pago inexistente |
| `infrastructure.webhook.WebhookDeliveryClientTest` | 5 | Firma contra el vector RFC 4231, payload y cabecera exactos, registro de 2xx, no-2xx y error de red, tolerancia a fallas de la bitácora |
| `infrastructure.webhook.WebhookRetryTest` | 5 | El notificador solo reintenta si falla el primer intento; cantidad de intentos; corte al tener éxito; lectura del *backoff* desde la configuración |
| `infrastructure.webhook.WebhookRetryAsyncTest` | 1 | Con el proxy real de Spring, los reintentos corren en otro hilo y la llamada vuelve de inmediato (L-01) |
| `infrastructure.security.AdminApiKeyInterceptorTest` | 1 | La aplicación no arranca con la API key vacía |
| `interfaces.rest.PaymentApiTest` | 18 | Contrato HTTP de las tres superficies, códigos 400/401/404/415/500, detalle de validación, checkout sin clave y sin reembolso, preflight CORS, intentos de evadir la API key con rutas codificadas o *matrix params* |

## 3. Estrategia por niveles

| Nivel | Alcance | Herramientas | Prioridad |
|---|---|---|---|
| Unitarias de dominio | `PaymentStatus`, `Payment`, `Money` (sin Spring) | JUnit 5, AssertJ | Alta |
| Casos de uso | Cada `*UseCase` con repositorio y notificador simulados | JUnit 5, Mockito | Alta |
| Firma de webhook | `WebhookNotifierAdapter.generateSignature` contra vectores conocidos | JUnit 5 | Alta |
| API (slice web) | Controladores + `GlobalExceptionHandler` | `@WebMvcTest`, MockMvc | Media |
| Integración con BD | Adaptador JPA, restricciones `CHECK`/`UNIQUE` | `@DataJpaTest` + Testcontainers PostgreSQL | Media |
| Webhook extremo a extremo | Entrega, reintentos y bitácora contra un receptor HTTP | WireMock / MockWebServer | Media |
| Checkout | Flujo en `pay.html` | Manual; Playwright a futuro | Baja |
| Contrato con el cliente | Backend TuEvento ↔ CPS | Prueba manual guiada (sección 6) | Alta |

## 4. Entorno de pruebas

- `docker compose up -d postgres-fake-gateway fake-payment-gateway`.
- Receptor de webhooks: backend TuEvento o el receptor mínimo de [04 §9](04-webhooks.md#9-probar-webhooks-sin-un-sistema-cliente).
- `WEBHOOK_SECRET` conocido por ambos lados.

## 5. Casos de prueba

| ID | Requisito | Descripción | Datos / pasos | Resultado esperado |
|---|---|---|---|---|
| CP-01 | RF-01 | Crear pago válido | `POST /public/payments` con los cuatro campos válidos | 201; `paymentId` empieza por `fake_`; `status=PENDING`; `currency` en mayúsculas |
| CP-02 | RF-01, RF-14 | Crear pago con campos faltantes o inválidos | Omitir `paymentMethod`, `amount: 0`, `currency: "COPX"` | 400 `Validation failed` con un mensaje por campo en `fields`; no se persiste nada |
| CP-03 | RF-01, RF-14 | Crear pago con enum inválido o JSON roto | `paymentMethod: "BITCOIN"`; cuerpo `{nope` | 400 `Malformed request`; con enum, el mensaje nombra el campo |
| CP-04 | RF-02 | Consultar pago existente | `GET /public/payments/{id}` | 200 con datos coincidentes |
| CP-05 | RF-02 | Consultar pago inexistente | `GET /public/payments/fake_no-existe` | 404 `Payment not found` |
| CP-06 | RF-03, RF-09 | Aprobar | `POST …/approve` sobre `PENDING` | 200; estado `APPROVED`; receptor recibe `payment.processing` y luego `payment.approved` con `eventId` distintos |
| CP-07 | RF-04 | Rechazar | `POST …/decline` | `DECLINED`; webhooks `processing` + `declined` |
| CP-08 | RF-05 | Fallar | `POST …/fail` | `FAILED`; webhooks `processing` + `failed` |
| CP-09 | RF-06 | Cancelar | `POST …/cancel` sobre `PENDING` | `CANCELLED`; un solo webhook `payment.cancelled` |
| CP-10 | RF-07 | Reembolsar aprobado | approve y luego refund | `REFUNDED`; webhook `payment.refunded` |
| CP-11 | RF-07, RF-08 | Reembolsar no aprobado | refund sobre `PENDING`, `DECLINED`, `REFUNDED` | 400 `Invalid status transition`; estado sin cambios; sin webhook |
| CP-12 | RF-08 | Transiciones inválidas | approve sobre `APPROVED`; cancel sobre `DECLINED`; decline sobre `CANCELLED` | 400 en todos; estado sin cambios |
| CP-13 | RF-09 | Firma correcta | Recalcular HMAC-SHA256 del cuerpo con el secreto | Coincide con `X-Signature` |
| CP-14 | RF-09 | Firma con secreto distinto | Configurar el receptor con otro secreto | El receptor rechaza (401) y CPS registra intentos con `http_status=401` |
| CP-15 | RF-10 | Reintentos | Receptor responde 500 | 3 registros en `webhook_delivery_logs` para el mismo `eventId`, separados ~2 s y ~10 s |
| CP-16 | RF-11 | Auditoría | Cualquier transición con receptor activo | Registro con `attempt_number=1`, `http_status=200` |
| CP-17 | RF-12 | Listar | Crear 3 pagos y `GET /admin/payments` | Arreglo con al menos esos 3 |
| CP-18 | IU-01, IU-02 | Checkout muestra el pago | Abrir `/pay.html?id={id}` | Estado, monto en formato moneda, método, referencia y fechas |
| CP-19 | IU-03 | Botones según estado | Abrir con pago `PENDING` y luego `APPROVED` | `PENDING`: 4 botones; `APPROVED`: mensaje terminal |
| CP-20 | IU-05 | Comunicación con ventana padre | Abrir como popup desde el frontend web y aprobar | La ventana padre recibe `GATEWAY_ACTION`; el popup se cierra a los ~1,5 s |
| CP-21 | IU-06, IU-07 | Acceso móvil y errores | Abrir por IP LAN desde un teléfono; abrir sin `id` | Carga correcta; mensaje de error sin `id` |
| CP-22 | RNF-04 | `CHECK` de estado | `UPDATE payments SET status='X'` directo en BD | La BD rechaza la sentencia |
| CP-23 | RNF-05 | Unicidad de `eventId` | Revisar `webhook_event_id` tras varias transiciones | Valor distinto en cada transición |
| CP-24 | RF-13 | `/admin` sin clave o con clave incorrecta | `POST /admin/payments/{id}/refund` sin `X-Api-Key` y con un valor erróneo | 401 `Unauthorized`; el caso de uso no se ejecuta |
| CP-25 | RF-13 | `/admin` con clave válida | `GET /admin/payments` y `POST …/refund` con `X-Api-Key` correcta | 200 |
| CP-26 | RF-14 | Error interno sin filtrar detalles | Provocar una excepción no controlada | 500 con `message: "Unexpected error"`; el detalle solo en el log |
| CP-27 | IU-03 | El checkout no puede reembolsar | `POST /public/checkout/{id}/refund` | 4xx; el caso de uso de reembolso no se ejecuta |
| CP-28 | RF-10, RNF-02 | Reintentos sin bloquear | Receptor caído; aprobar un pago | La petición responde sin esperar los ~12 s de *backoff*; los intentos 2 y 3 aparecen después en la bitácora |

## 6. Prueba de aceptación extremo a extremo (TuEvento)

1. Iniciar sesión como asistente en la web, elegir un evento y asientos, confirmar compra.
2. Se abre el popup del checkout → **Aprobar**.
3. Verificar: popup cerrado, orden pagada, boletas con QR emitidas.
4. Repetir con **Rechazar**, **Fallar** y **Cancelar** → la orden no queda pagada y los asientos se liberan.
5. Repetir el paso 1–3 desde la app móvil usando la IP LAN.
6. Como organizador, cancelar el evento con una orden pagada → CPS recibe `refund`, emite `payment.refunded` y la boleta queda reembolsada.

## 7. Criterios de entrada y salida

- **Entrada:** build exitoso, servicios levantados, secreto configurado.
- **Salida (release):** 100 % de los casos de prioridad alta aprobados; sin defectos críticos abiertos; defectos menores registrados en [10](10-limitaciones-y-roadmap.md).

## 8. Cobertura de casos por pruebas automatizadas

| Casos | Cubiertos por |
|---|---|
| CP-01, CP-02, CP-03, CP-04, CP-05 | `PaymentApiTest`, `PaymentUseCasesTest` |
| CP-06 … CP-12 | `PaymentUseCasesTest`, `PaymentTest`, `PaymentStatusTest`, `PaymentApiTest` |
| CP-13, CP-14, CP-16 | `WebhookDeliveryClientTest` (con el receptor simulado por `MockRestServiceServer`) |
| CP-15, CP-28 | `WebhookRetryTest`, `WebhookRetryAsyncTest` |
| CP-17, CP-24 … CP-27 | `PaymentApiTest`, `AdminApiKeyInterceptorTest` |
| CP-18 … CP-21 | Manual (checkout en navegador y móvil) |
| CP-22, CP-23 | Manual contra PostgreSQL (L-14) |
