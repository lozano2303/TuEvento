# 04 — Especificación de webhooks

**Producto:** CapySoft Payment Sandbox (CPS) 1.0.0 · **Versión del documento:** 1.1 · **Fecha:** 2026-10-24

---

## 1. Propósito

CPS informa al sistema cliente cada cambio de estado de un pago mediante una petición HTTP POST a una URL que el cliente expone. Es el mismo modelo que usan las pasarelas comerciales: el cliente **no** debe confiar en lo que le diga el navegador del usuario, sino en esta notificación firmada servidor a servidor.

## 2. Configuración

| Variable | Descripción | Valor por defecto |
|---|---|---|
| `PAYMENT_CALLBACK_URL` | URL que recibe los webhooks | `http://localhost:8080/api/v1/webhooks/payment` |
| `WEBHOOK_SECRET` | Secreto compartido para la firma HMAC | `default-secret-change-in-production` (**debe cambiarse**) |
| `WEBHOOK_CONNECT_TIMEOUT_MS` | *Timeout* de conexión por intento | `3000` |
| `WEBHOOK_READ_TIMEOUT_MS` | *Timeout* de lectura por intento | `5000` |
| `WEBHOOK_RETRY_BACKOFF_MS` | Esperas antes de cada reintento, separadas por coma; intentos totales = elementos + 1 | `2000,10000` |

El mismo secreto debe estar configurado en el sistema cliente (en TuEvento: `payment.webhook.secret` / variable `WEBHOOK_SECRET`).

## 3. Catálogo de eventos

| Evento (`event`) | `status` | Cuándo ocurre |
|---|---|---|
| `payment.processing` | `PROCESSING` | Inicio de una autorización (antes de approve, decline o fail) |
| `payment.approved` | `APPROVED` | El cobro fue aprobado |
| `payment.declined` | `DECLINED` | El emisor rechazó el cobro |
| `payment.failed` | `FAILED` | Error técnico; el cobro no se realizó |
| `payment.cancelled` | `CANCELLED` | El pago se canceló antes de procesarse |
| `payment.refunded` | `REFUNDED` | Un pago aprobado fue reembolsado en su totalidad |

El nombre del evento se deriva como `"payment." + status.toLowerCase()`, de modo que un estado nuevo produce automáticamente un evento nuevo con el mismo contrato.

> No hay evento de creación: el cliente ya conoce el estado `PENDING` por la respuesta de `POST /public/payments`.

## 4. Petición

```http
POST /api/v1/webhooks/payment HTTP/1.1
Content-Type: application/json
X-Signature: 9a1c5e0b7f3d2a6e4c8b1f0d3e5a7c9b2d4f6a8c0e1b3d5f7a9c1e3b5d7f9a1c

{"eventId":"5b0e9a52-7f0c-4a8a-9a3e-0c1d2e3f4a5b","event":"payment.approved","paymentId":"fake_3f2b7a0e-9c1d-4e8f-a6b5-1d2c3e4f5a6b","status":"APPROVED","amount":150000.0000,"currency":"COP","timestamp":"2026-10-24T15:05:10.987654Z"}
```

| Campo | Tipo | Descripción |
|---|---|---|
| `eventId` | string (UUID) | Único por transición. **Clave de idempotencia.** |
| `event` | string | Tipo de evento (sección 3) |
| `paymentId` | string | Identificador del pago en CPS |
| `status` | string | Estado nuevo |
| `amount` | number | Monto del pago |
| `currency` | string | Moneda ISO 4217 |
| `timestamp` | string (ISO 8601 UTC) | Momento de la transición |

El cuerpo se envía compacto (sin espacios ni saltos de línea) y en el orden de campos mostrado.

## 5. Firma

```
X-Signature = hex_minúsculas( HMAC_SHA256( clave = WEBHOOK_SECRET (UTF-8), mensaje = cuerpo_crudo (UTF-8) ) )
```

Reglas para el receptor:

1. Calcule la firma sobre el **cuerpo crudo tal como llegó**, antes de deserializarlo. Re-serializar el JSON puede alterar el orden de campos o el formato de números (`150000.0000` → `150000.0`) y la firma ya no coincidirá.
2. Compare en **tiempo constante** (`MessageDigest.isEqual`, `crypto.timingSafeEqual`, `hmac.compare_digest`).
3. Si la firma no coincide, responda `401` o `400` y descarte el mensaje.

### 5.1 Ejemplos de verificación

**Java (Spring)**

```java
@PostMapping("/api/v1/webhooks/payment")
public ResponseEntity<Void> recibir(@RequestBody String body,
                                    @RequestHeader("X-Signature") String firma) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    String esperada = HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    if (!MessageDigest.isEqual(esperada.getBytes(), firma.getBytes())) {
        return ResponseEntity.status(401).build();
    }
    // ... idempotencia por eventId y procesamiento
    return ResponseEntity.ok().build();
}
```

**Node.js (Express)**

```js
app.post('/api/v1/webhooks/payment', express.raw({ type: 'application/json' }), (req, res) => {
  const esperada = crypto.createHmac('sha256', process.env.WEBHOOK_SECRET).update(req.body).digest('hex');
  const recibida = req.get('X-Signature') || '';
  const ok = recibida.length === esperada.length &&
             crypto.timingSafeEqual(Buffer.from(esperada), Buffer.from(recibida));
  if (!ok) return res.sendStatus(401);
  const evento = JSON.parse(req.body);
  // ... idempotencia por evento.eventId
  res.sendStatus(200);
});
```

**Python (FastAPI)**

```python
@app.post("/api/v1/webhooks/payment")
async def recibir(request: Request, x_signature: str = Header(...)):
    cuerpo = await request.body()
    esperada = hmac.new(SECRETO.encode(), cuerpo, hashlib.sha256).hexdigest()
    if not hmac.compare_digest(esperada, x_signature):
        raise HTTPException(status_code=401)
    evento = json.loads(cuerpo)
    # ... idempotencia por evento["eventId"]
    return {"ok": True}
```

## 6. Respuesta esperada y reintentos

- Cualquier **2xx** se considera entrega exitosa. El cuerpo de la respuesta se ignora.
- Cualquier otro código, error de conexión o excepción se considera fallo.

| Intento | Espera previa |
|---|---|
| 1 | — (inmediato) |
| 2 | 2 s |
| 3 | 10 s |

- El **intento 1** es síncrono, dentro de la petición que provocó la transición, con *timeouts* de 3 s (conexión) y 5 s (lectura).
- Si falla, la petición responde de inmediato y los **intentos 2 y 3** se ejecutan en el pool asíncrono de Spring (`WebhookRetryScheduler`), sin bloquear al llamador.
- Tras el último fallo CPS registra `Webhook delivery failed after 3 attempts` en el log y no vuelve a intentar. El estado del pago en CPS **no** se revierte: el cliente puede reconciliar consultando `GET /public/payments/{id}`.
- Los reintentos pendientes viven en memoria: si CPS se reinicia durante la espera, se pierden (L-13 en [10](10-limitaciones-y-roadmap.md)). La reconciliación cubre ese caso.

## 7. Garantías de entrega e idempotencia

- **Al menos una vez (best effort, 3 intentos):** un mismo `eventId` puede llegar más de una vez (p. ej. el cliente procesó pero respondió tarde). El cliente **DEBE** registrar los `eventId` procesados e ignorar duplicados.
- **Orden:** dentro de una misma petición los eventos se envían en orden (`processing` antes que `approved`). Entre peticiones distintas no se garantiza orden si hay reintentos; el cliente DEBERÍA ignorar transiciones que no tengan sentido desde su estado actual.
- **Consistencia:** el `PROCESSING` se notifica antes de que la transacción de base de datos haga *commit*. Si el cliente consulta la API en ese instante puede ver todavía `PENDING`. Trate el webhook como fuente de verdad del cambio.

## 8. Auditoría

Cada intento queda registrado en la tabla `webhook_delivery_logs`:

```sql
SELECT event_id, attempt_number, http_status, error_message, sent_at
FROM webhook_delivery_logs
ORDER BY sent_at DESC
LIMIT 20;
```

## 9. Probar webhooks sin un sistema cliente

Levante un receptor mínimo y apunte `PAYMENT_CALLBACK_URL` a él:

```bash
# Terminal 1: receptor que imprime cabeceras y cuerpo
python -c "
from http.server import BaseHTTPRequestHandler, HTTPServer
class H(BaseHTTPRequestHandler):
    def do_POST(self):
        n=int(self.headers['Content-Length']); print(self.headers['X-Signature'], self.rfile.read(n).decode())
        self.send_response(200); self.end_headers()
HTTPServer(('0.0.0.0', 9999), H).serve_forever()"

# Terminal 2: CPS apuntando al receptor
PAYMENT_CALLBACK_URL=http://localhost:9999/ WEBHOOK_SECRET=demo ADMIN_API_KEY=dev-admin-key-change-in-production ./mvnw spring-boot:run
```

Responder `500` en el receptor permite observar los reintentos y su registro en la bitácora.
