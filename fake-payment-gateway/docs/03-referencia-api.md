# 03 — Referencia de la API

**Producto:** CapySoft Payment Sandbox (CPS) 1.0.0 · **Versión del documento:** 1.1 · **Fecha:** 2026-10-24

---

## 1. Generalidades

| Aspecto | Valor |
|---|---|
| URL base (local) | `http://localhost:4001` |
| Formato | JSON UTF-8 (`Content-Type: application/json`) |
| Fechas | ISO 8601 en UTC, p. ej. `2026-10-24T15:04:05.123456Z` |
| Montos | Número decimal; se almacenan con 4 decimales (al consultar, `150000` vuelve como `150000.0000`) |
| Moneda | Código ISO 4217 de 3 letras; se normaliza a mayúsculas |
| Identificador de pago | `fake_` + UUID v4 (41 caracteres) |
| CORS | Abierto a cualquier origen; métodos `GET, POST, PUT, DELETE, OPTIONS` |

La API tiene tres superficies:

| Superficie | Prefijo | Autenticación | Quién la usa |
|---|---|---|---|
| Pública | `/public/payments` | Ninguna | Sistema cliente: crear y consultar pagos |
| Checkout | `/public/checkout/{paymentId}` | Posesión del `paymentId` | Página `pay.html`: el pagador decide el desenlace |
| Administrativa | `/admin/payments` | Cabecera `X-Api-Key` | Sistema cliente (cancelar, reembolsar), QA, scripts |

### 1.1 Autenticación de `/admin/**`

Toda petición a `/admin/**` debe incluir:

```http
X-Api-Key: <valor de ADMIN_API_KEY>
```

- La clave se configura en CPS con la variable `ADMIN_API_KEY` y en el sistema cliente (TuEvento: `FAKE_GATEWAY_API_KEY`).
- La comparación es en tiempo constante.
- Sin cabecera o con una clave incorrecta, CPS responde `401` sin ejecutar la operación:

  ```json
  { "error": "Unauthorized", "message": "Missing or invalid X-Api-Key header", "timestamp": "…" }
  ```

- Las peticiones `OPTIONS` (preflight de CORS) no requieren clave.
- CPS no arranca si `ADMIN_API_KEY` está vacía, y registra una advertencia si se usa el valor de desarrollo por defecto.

### 1.2 Por qué el checkout es público

En cualquier pasarela, la página de pago la usa un pagador anónimo. CPS sigue ese modelo: las acciones del checkout solo requieren conocer el `paymentId`, un UUID v4 con 122 bits aleatorios que no se puede adivinar ni enumerar (el listado está protegido). El **reembolso no está en el checkout**: es una operación del comercio y solo existe en `/admin`.

## 2. Recurso `Payment`

```json
{
  "paymentId": "fake_3f2b7a0e-9c1d-4e8f-a6b5-1d2c3e4f5a6b",
  "externalReference": "ORDER-2026-000123",
  "status": "PENDING",
  "amount": 150000.0000,
  "currency": "COP",
  "paymentMethod": "CARD",
  "createdAt": "2026-10-24T15:04:05.123456Z",
  "updatedAt": "2026-10-24T15:04:05.123456Z"
}
```

| Campo | Tipo | Descripción |
|---|---|---|
| `paymentId` | string | Identificador asignado por CPS |
| `externalReference` | string | Identificador del pago u orden en el sistema cliente |
| `status` | enum | `PENDING`, `PROCESSING`, `APPROVED`, `DECLINED`, `FAILED`, `CANCELLED`, `REFUNDED` |
| `amount` | number | Monto |
| `currency` | string | Moneda ISO 4217 |
| `paymentMethod` | enum | `QR`, `CARD`, `PSE` |
| `createdAt` | string (fecha) | Creación |
| `updatedAt` | string (fecha) | Última transición |

Significado de cada estado: [05 — Modelo de datos](05-modelo-de-datos.md#3-máquina-de-estados).

## 3. API pública

### 3.1 Crear pago

`POST /public/payments`

| Campo | Tipo | Obligatorio | Reglas |
|---|---|---|---|
| `externalReference` | string | Sí | No vacío; máx. 100 caracteres |
| `amount` | number | Sí | > 0 |
| `currency` | string | Sí | Exactamente 3 letras (ISO 4217) |
| `paymentMethod` | string | Sí | `QR` \| `CARD` \| `PSE` |

```bash
curl -X POST http://localhost:4001/public/payments \
  -H "Content-Type: application/json" \
  -d '{"externalReference":"ORDER-2026-000123","amount":150000,"currency":"cop","paymentMethod":"CARD"}'
```

**`201 Created`**

```json
{
  "paymentId": "fake_3f2b7a0e-9c1d-4e8f-a6b5-1d2c3e4f5a6b",
  "externalReference": "ORDER-2026-000123",
  "status": "PENDING",
  "amount": 150000,
  "currency": "COP",
  "paymentMethod": "CARD",
  "createdAt": "2026-10-24T15:04:05.123456Z",
  "updatedAt": "2026-10-24T15:04:05.123456Z"
}
```

**`400 Bad Request`** (validación)

```json
{
  "error": "Validation failed",
  "message": "One or more fields are invalid",
  "timestamp": "2026-10-24T15:04:05.123Z",
  "fields": {
    "amount": "Amount must be positive",
    "currency": "Currency must be a 3-letter ISO 4217 code"
  }
}
```

La creación **no** emite webhook. Para llevar al usuario al checkout, construya `{URL_BASE}/pay.html?id={paymentId}`.

### 3.2 Consultar pago

`GET /public/payments/{paymentId}` → `200 OK` con el recurso · `404 Not Found` si no existe.

## 4. Checkout

Todas responden `200 OK` sin cuerpo si la transición se aplicó.

| Método y ruta | Precondición | Transiciones | Webhooks |
|---|---|---|---|
| `POST /public/checkout/{id}/approve` | `PENDING` | `PENDING → PROCESSING → APPROVED` | `payment.processing`, `payment.approved` |
| `POST /public/checkout/{id}/decline` | `PENDING` | `PENDING → PROCESSING → DECLINED` | `payment.processing`, `payment.declined` |
| `POST /public/checkout/{id}/fail` | `PENDING` | `PENDING → PROCESSING → FAILED` | `payment.processing`, `payment.failed` |
| `POST /public/checkout/{id}/cancel` | `PENDING` | `PENDING → CANCELLED` | `payment.cancelled` |

- **approve:** el emisor autorizó el cobro.
- **decline:** el emisor rechazó el cobro (fondos insuficientes, tarjeta bloqueada, etc.).
- **fail:** error técnico en la red de pagos; el cobro no se realizó.
- **cancel:** el pagador abandonó el pago.

## 5. API administrativa (requiere `X-Api-Key`)

| Método y ruta | Precondición | Transiciones | Webhooks |
|---|---|---|---|
| `GET /admin/payments` | — | — | — |
| `GET /admin/payments/{id}` | — | — | — |
| `POST /admin/payments/{id}/approve` | `PENDING` | igual que en el checkout | igual que en el checkout |
| `POST /admin/payments/{id}/decline` | `PENDING` | igual que en el checkout | igual que en el checkout |
| `POST /admin/payments/{id}/fail` | `PENDING` | igual que en el checkout | igual que en el checkout |
| `POST /admin/payments/{id}/cancel` | `PENDING` | `PENDING → CANCELLED` | `payment.cancelled` |
| `POST /admin/payments/{id}/refund` | `APPROVED` | `APPROVED → REFUNDED` | `payment.refunded` |

Las acciones de aprobar, rechazar y fallar existen también aquí para que QA y los scripts de prueba automatizados puedan decidir el desenlace sin abrir el navegador.

```bash
KEY=dev-admin-key-change-in-production
curl -H "X-Api-Key: $KEY" http://localhost:4001/admin/payments
curl -X POST -H "X-Api-Key: $KEY" http://localhost:4001/admin/payments/fake_3f2b.../cancel
curl -X POST -H "X-Api-Key: $KEY" http://localhost:4001/admin/payments/fake_3f2b.../refund
```

`GET /admin/payments` devuelve todos los pagos, sin paginación ni orden garantizado.

## 6. Errores

```json
{ "error": "Invalid status transition", "message": "Cannot transition from DECLINED to REFUNDED", "timestamp": "…" }
```

| HTTP | `error` | Causa |
|---|---|---|
| 400 | `Validation failed` | Uno o más campos no cumplen las reglas; detalle en `fields` |
| 400 | `Malformed request` | JSON inválido, cuerpo ausente o valor fuera del enum (`message` indica el campo) |
| 400 | `Invalid status transition` | La transición no está permitida desde el estado actual |
| 400 | `Invalid argument` | Invariante de dominio violada |
| 401 | `Unauthorized` | Falta `X-Api-Key` o es incorrecta (solo `/admin/**`) |
| 404 | `Payment not found` | `paymentId` inexistente |
| 404 | `Not found` | Ruta o recurso estático inexistente |
| 405 | `Method not allowed` | Método HTTP no soportado en la ruta |
| 409 | `Data integrity violation` | La operación viola una restricción de la base de datos |
| 415 | `Unsupported media type` | El cuerpo no es `application/json` |
| 500 | `Internal server error` | Error no controlado; el mensaje es genérico (`Unexpected error`) y el detalle queda en el log |

## 7. Checkout hospedado

`GET /pay.html?id={paymentId}` — página HTML pública.

- Carga el pago y lo vuelve a consultar cada 3 s mientras no esté en estado terminal.
- Ofrece Aprobar, Rechazar, Fallar y Cancelar mientras el pago esté en `PENDING`, usando los endpoints de la sección 4.
- Tras una acción, si fue abierta con `window.open`, envía a la ventana padre

  ```js
  { type: 'GATEWAY_ACTION', action: 'approve' | 'decline' | 'fail' | 'cancel' }
  ```

  y se cierra a los 1,5 s. El sistema cliente **no** debe tomar este mensaje como confirmación de pago: la fuente de verdad es el webhook firmado o una consulta a la API.
- Usa el mismo origen desde el que se cargó para llamar a la API, así que funciona abierta desde un teléfono por la IP de la red local (`http://192.168.x.x:4001/pay.html?id=...`).

## 8. Colección de pruebas rápida

```bash
BASE=http://localhost:4001
KEY=dev-admin-key-change-in-production
ID=$(curl -s -X POST $BASE/public/payments -H "Content-Type: application/json" \
  -d '{"externalReference":"DEMO-1","amount":50000,"currency":"COP","paymentMethod":"PSE"}' \
  | sed -E 's/.*"paymentId":"([^"]+)".*/\1/')
echo "Checkout: $BASE/pay.html?id=$ID"
curl -s -X POST $BASE/public/checkout/$ID/approve -o /dev/null -w "approve              → %{http_code}\n"
curl -s -X POST $BASE/admin/payments/$ID/refund  -o /dev/null -w "refund sin clave     → %{http_code}\n"
curl -s -X POST -H "X-Api-Key: $KEY" $BASE/admin/payments/$ID/refund -o /dev/null -w "refund con clave     → %{http_code}\n"
curl -s -X POST -H "X-Api-Key: $KEY" $BASE/admin/payments/$ID/refund -w "\nrefund otra vez      → %{http_code}\n"
curl -s $BASE/public/payments/$ID
```

Resultado esperado: `200`, `401`, `200`, `400` y el pago en `REFUNDED`.
