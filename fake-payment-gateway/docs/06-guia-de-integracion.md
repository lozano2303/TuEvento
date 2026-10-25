# 06 — Guía de integración

**Producto:** CapySoft Payment Sandbox (CPS) 1.0.0 · **Versión del documento:** 1.0 · **Fecha:** 2026-10-24

Esta guía lleva a un equipo desde cero hasta un flujo de cobro funcional con CPS. Se usa la integración de TuEvento como implementación de referencia.

---

## 1. Requisitos previos

- Docker y Docker Compose, o Java 21 + PostgreSQL 17.
- Un endpoint HTTP en su sistema para recibir webhooks.
- Un secreto aleatorio compartido, por ejemplo: `openssl rand -hex 32`.

## 2. Paso 1 — Levantar CPS

```bash
docker compose up -d postgres-fake-gateway fake-payment-gateway
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:4001/pay.html   # 200
```

Variables mínimas (detalle en [07](07-despliegue-y-operacion.md)):

```env
PAYMENT_CALLBACK_URL=http://<host-de-su-sistema>:<puerto>/<ruta-webhook>
WEBHOOK_SECRET=<su-secreto>
ADMIN_API_KEY=<su-api-key>        # openssl rand -hex 32
```

Si su sistema corre en el host y CPS en Docker, use `host.docker.internal` en la URL de webhook.

## 3. Paso 2 — Aislar la pasarela detrás de un puerto

No llame a CPS desde la lógica de negocio. Defina una interfaz propia y haga de CPS un adaptador; así podrá cambiar a un proveedor productivo sin tocar el resto del sistema.

Referencia en TuEvento:

```java
// application/port/out — propiedad del sistema cliente
public interface PaymentGatewayPort {
    GatewayPayment createPayment(CreatePaymentCommand command);
    GatewayPayment getPayment(String gatewayPaymentId);
    void cancelPayment(String gatewayPaymentId);
    void refundPayment(String gatewayPaymentId);
    GatewayPaymentEvent processWebhook(String payload, String signature);
}

// infrastructure/external — adaptador de CPS, activable por configuración
@Component
@ConditionalOnProperty(name = "payment.gateway", havingValue = "fake")
public class FakePaymentGatewayAdapter implements PaymentGatewayPort { ... }
```

```yaml
payment:
  gateway: ${PAYMENT_GATEWAY:fake}          # otro valor activa otro adaptador
  fake-gateway:
    url: ${FAKE_GATEWAY_URL:http://localhost:4001}
    api-key: ${FAKE_GATEWAY_API_KEY}        # igual a ADMIN_API_KEY de CPS
  webhook:
    secret: ${WEBHOOK_SECRET}
```

## 4. Paso 3 — Crear el pago

Al confirmar la orden en su sistema:

1. Persista la orden en estado "pendiente de pago" con un identificador propio.
2. Llame a `POST /public/payments` con ese identificador como `externalReference`.
3. Guarde el `paymentId` devuelto junto a la orden.

```json
POST /public/payments
{ "externalReference": "ORDER-2026-000123", "amount": 150000, "currency": "COP", "paymentMethod": "CARD" }
```

## 5. Paso 4 — Llevar al usuario al checkout

URL: `{FAKE_GATEWAY_URL}/pay.html?id={paymentId}`

**Web — popup** (referencia: `TuEventoFrontend/web/src/pages/PaymentPending.jsx`)

```js
const popup = window.open(`${GATEWAY_BASE_URL}/pay.html?id=${paymentId}`, 'cps', 'width=480,height=720');

window.addEventListener('message', (event) => {
  if (event.data?.type !== 'GATEWAY_ACTION') return;
  // Solo es una pista para refrescar la UI: confirme el estado con su backend.
  refrescarEstadoDeLaOrden();
});
```

**Móvil — WebView o navegador del sistema** (referencia: `TuEventoFrontend/movil/src/screens/PaymentPendingScreen.jsx`)

Use la IP de la red local en lugar de `localhost` (en el teléfono, `localhost` es el propio teléfono). El checkout usa su propio origen para llamar a la API, así que no requiere configuración adicional.

## 6. Paso 5 — Recibir y verificar webhooks

Su endpoint debe, en este orden:

1. Leer el **cuerpo crudo**.
2. Verificar `X-Signature` (HMAC-SHA256 hex) en tiempo constante → 401 si no coincide.
3. Buscar el `eventId` en su registro de eventos procesados → si existe, responder 200 sin hacer nada.
4. Ubicar la orden por `paymentId`.
5. Aplicar el cambio según `status`:

   | `status` | Acción sugerida en el sistema cliente |
   |---|---|
   | `PROCESSING` | Opcional: marcar "procesando" |
   | `APPROVED` | Confirmar la orden y entregar el producto (en TuEvento: emitir boletas) |
   | `DECLINED` / `FAILED` | Marcar la orden como no pagada y liberar inventario reservado |
   | `CANCELLED` | Igual que el anterior |
   | `REFUNDED` | Revertir la entrega (invalidar boletas) y registrar el reembolso |

6. Guardar el `eventId` y responder 2xx en menos de unos segundos (haga el trabajo pesado de forma asíncrona).

Código de verificación en Java, Node.js y Python: [04 — Webhooks §5.1](04-webhooks.md#51-ejemplos-de-verificación).

## 7. Paso 6 — Cancelaciones y reembolsos desde su sistema

- Orden expirada antes de pagar → `POST /admin/payments/{paymentId}/cancel`.
- Devolución de una orden pagada → `POST /admin/payments/{paymentId}/refund`.

Ambas exigen la cabecera `X-Api-Key` (un 401 indica que falta o no coincide con `ADMIN_API_KEY`), responden 200 sin cuerpo y emiten el webhook correspondiente. Un 400 indica que el pago ya no está en un estado que permita la operación; consulte su estado y reconcilie.

## 8. Paso 7 — Reconciliación

Si su sistema estuvo caído y perdió webhooks (CPS solo intenta 3 veces), reconcilie periódicamente las órdenes pendientes con `GET /public/payments/{paymentId}`.

## 9. Lista de verificación de la integración

- [ ] El código de negocio depende de una interfaz propia, no de CPS.
- [ ] `WEBHOOK_SECRET` y la API key no están en el repositorio y coinciden en ambos lados.
- [ ] Las llamadas a `/admin/**` envían `X-Api-Key`.
- [ ] La firma se calcula sobre el cuerpo crudo y se compara en tiempo constante.
- [ ] Los webhooks se procesan de forma idempotente por `eventId`.
- [ ] El `postMessage` del checkout no se usa como confirmación de pago.
- [ ] Se probaron los cinco desenlaces: aprobado, rechazado, fallido, cancelado, reembolsado.
- [ ] Se probó que un webhook con firma alterada es rechazado.
- [ ] Existe reconciliación para órdenes pendientes.

## 10. Migrar a un proveedor productivo

1. Implemente un nuevo adaptador de `PaymentGatewayPort` para el proveedor (por ejemplo Wompi).
2. Mapee sus estados al modelo de su sistema (los estados de CPS se inspiran en los de las pasarelas comerciales, por lo que el mapeo suele ser directo).
3. Cambie `payment.gateway` en el entorno productivo.
4. Mantenga CPS en desarrollo, QA y demos.
