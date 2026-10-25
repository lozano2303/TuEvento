# 07 — Despliegue y operación

**Producto:** CapySoft Payment Sandbox (CPS) 1.0.0 · **Versión del documento:** 1.1 · **Fecha:** 2026-10-24

---

## 1. Requisitos

| Recurso | Mínimo recomendado |
|---|---|
| CPU | 0,25 vCPU |
| Memoria | 512 MiB para la JVM |
| PostgreSQL | 17 (base dedicada, p. ej. `fake_gateway_db`) |
| Puertos | 4001/TCP entrante (configurable); salida HTTP hacia la URL de webhook |

## 2. Variables de entorno

| Variable | Obligatoria | Por defecto | Descripción |
|---|---|---|---|
| `FAKE_GATEWAY_PORT` | No | `4001` | Puerto HTTP |
| `POSTGRES_HOST` | No | `localhost` | Host de PostgreSQL |
| `POSTGRES_PORT` | No | `5432` | Puerto de PostgreSQL |
| `POSTGRES_DB` | No | `fake_gateway_db` | Base de datos |
| `POSTGRES_USER` | No | `postgres` | Usuario |
| `POSTGRES_PASSWORD` | Sí en entornos compartidos | `postgres` | Contraseña |
| `PAYMENT_CALLBACK_URL` | Sí | `http://localhost:8080/api/v1/webhooks/payment` | Destino de los webhooks |
| `WEBHOOK_SECRET` | Sí | `default-secret-change-in-production` | Secreto HMAC; **nunca** use el valor por defecto fuera de local |
| `ADMIN_API_KEY` | **Sí** | — (sin valor por defecto) | API key de `/admin/**` (cabecera `X-Api-Key`), mínimo 16 caracteres. Si falta, está vacía o es más corta, CPS no arranca. La clave de desarrollo `dev-admin-key-change-in-production` (usada por `docker-compose.yml`) produce una advertencia en el log |
| `WEBHOOK_CONNECT_TIMEOUT_MS` | No | `3000` | *Timeout* de conexión de cada intento de webhook |
| `WEBHOOK_READ_TIMEOUT_MS` | No | `5000` | *Timeout* de lectura de cada intento de webhook |
| `WEBHOOK_RETRY_BACKOFF_MS` | No | `2000,10000` | Esperas antes de cada reintento; intentos totales = elementos + 1 |
| `LOG_LEVEL` | No | `INFO` | Nivel de log de `com.capysoft.fakepaymentgateway` y `org.springframework.web` |
| `SHOW_SQL` | No | `false` | Muestra SQL generado por Hibernate |

Plantilla: [`.env.example`](../.env.example). El archivo `.env` está en `.gitignore`.

## 3. Ejecución local sin Docker

```bash
cd fake-payment-gateway
cp .env.example .env            # ajuste valores
# exporte las variables (o use su IDE); ADMIN_API_KEY es obligatoria, p. ej.:
# export ADMIN_API_KEY=dev-admin-key-change-in-production
./mvnw spring-boot:run

# pruebas automatizadas (no requieren base de datos)
./mvnw test
```

## 4. Imagen Docker

`Dockerfile` multi-etapa:

1. **builder** — `eclipse-temurin:21-jdk-alpine` + Maven: `mvn clean package -DskipTests`.
2. **runtime** — `eclipse-temurin:21-jre-alpine` con solo `app.jar`; expone 4001.

```bash
docker build -t capysoft/payment-sandbox:1.0.0 fake-payment-gateway
docker run -d --name cps -p 4001:4001 \
  -e POSTGRES_HOST=host.docker.internal -e POSTGRES_PORT=5435 \
  -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=*** \
  -e PAYMENT_CALLBACK_URL=http://host.docker.internal:8080/api/v1/webhooks/payment \
  -e WEBHOOK_SECRET=*** \
  -e ADMIN_API_KEY=*** \
  capysoft/payment-sandbox:1.0.0
```

## 5. Docker Compose (monorepo TuEvento)

Servicios en `docker-compose.yml` de la raíz:

| Servicio | Imagen | Puerto host | Notas |
|---|---|---|---|
| `postgres-fake-gateway` | `postgres:17-alpine` | 5435 | Volumen `postgres_fake_gateway_data` |
| `fake-payment-gateway` | build de `./fake-payment-gateway` | 4001 | `PAYMENT_CALLBACK_URL=http://host.docker.internal:8080/api/v1/webhooks/payment`; `ADMIN_API_KEY` toma `FAKE_GATEWAY_API_KEY` del `.env` raíz (el mismo valor que usa el backend) |

```bash
docker compose up -d postgres-fake-gateway fake-payment-gateway
docker compose logs -f fake-payment-gateway
```

## 6. Nube

- **Render / PaaS:** desplegar como *Web Service* Docker apuntando a `fake-payment-gateway/Dockerfile`, con una base PostgreSQL administrada y las variables de la sección 2. El backend cliente apunta su `FAKE_GATEWAY_URL` a la URL pública asignada (en el plan de TuEvento: `https://tuevento-fake-gateway.onrender.com`) y configura `FAKE_GATEWAY_API_KEY` con el mismo valor que `ADMIN_API_KEY`.
- **Kubernetes:** un `Deployment` con 1 réplica, un `Service` ClusterIP en 4001, `Secret` para `POSTGRES_PASSWORD`, `WEBHOOK_SECRET` y `ADMIN_API_KEY`, y `ConfigMap` para el resto. `/admin/**` queda protegido por API key; como defensa en profundidad se puede exponer por `Ingress` solo `/pay.html` y `/public/**` (ver [09](09-seguridad.md)).

> Mantenga **una sola réplica** en 1.0.0: no hay control de concurrencia optimista sobre los pagos (L-08).

## 7. Verificación tras el despliegue (smoke test)

```bash
BASE=https://<host-cps>
curl -fsS "$BASE/pay.html" > /dev/null && echo "checkout OK"
curl -fsS -X POST "$BASE/public/payments" -H "Content-Type: application/json" \
  -d '{"externalReference":"SMOKE","amount":1000,"currency":"COP","paymentMethod":"QR"}'
```

Luego apruebe el pago (`POST $BASE/public/checkout/{id}/approve`) y confirme en los logs `Webhook delivered successfully`. Verifique también que `curl -i $BASE/admin/payments` sin cabecera devuelve `401`.

## 8. Observabilidad

| Fuente | Qué buscar |
|---|---|
| Log de aplicación | `Webhook delivered successfully: eventId=…, attempt=…` · `Webhook returned non-2xx status` · `Webhook delivery failed` · `Webhook delivery failed after 3 attempts` · `Rejected admin request without valid API key` · `ADMIN_API_KEY is using the development default` |
| `webhook_delivery_logs` | Intentos con `http_status` nulo o distinto de 2xx |
| `payments` | Distribución de estados; pagos `PENDING` antiguos |

Consultas útiles:

```sql
-- Pagos por estado
SELECT status, count(*) FROM payments GROUP BY status;

-- Webhooks que nunca se entregaron
SELECT event_id, max(attempt_number) AS intentos, max(sent_at) AS ultimo
FROM webhook_delivery_logs
GROUP BY event_id
HAVING bool_and(http_status IS NULL OR http_status NOT BETWEEN 200 AND 299);
```

## 9. Respaldo y retención

Los datos de CPS son de prueba. Se recomienda no respaldarlos y purgar periódicamente:

```sql
DELETE FROM webhook_delivery_logs WHERE sent_at < now() - interval '30 days';
DELETE FROM payments WHERE updated_at < now() - interval '90 days';
```

## 10. Solución de problemas

| Síntoma | Causa probable | Acción |
|---|---|---|
| CPS no arranca: `Connection refused` a Postgres | Base de datos no lista o host/puerto incorrectos | Verifique `POSTGRES_HOST`/`PORT`; dentro de Compose use `postgres-fake-gateway:5432` |
| El cliente responde 401 a los webhooks | Secretos distintos, o el cliente firma un JSON re-serializado | Igualar `WEBHOOK_SECRET`; firmar el cuerpo crudo |
| Webhooks con `Connection refused` | `PAYMENT_CALLBACK_URL` apunta a `localhost` desde dentro del contenedor | Use `host.docker.internal` o el nombre del servicio |
| Aprobar tarda hasta ~10 s | URL de webhook que acepta la conexión pero no responde: el primer intento espera el *timeout* de lectura por cada evento | Corregir el receptor o bajar `WEBHOOK_READ_TIMEOUT_MS` |
| El backend recibe 401 al cancelar o reembolsar | `FAKE_GATEWAY_API_KEY` del backend distinta de `ADMIN_API_KEY` de CPS | Igualar ambos valores |
| CPS no arranca: `Could not resolve placeholder 'ADMIN_API_KEY'` | Variable no definida | Definir `ADMIN_API_KEY` |
| CPS no arranca: `ADMIN_API_KEY ... must not be blank` o `must be at least 16 characters` | Variable vacía o demasiado corta | Usar un valor aleatorio, p. ej. `openssl rand -hex 32` |
| Error `violates check constraint "payments_status_check"` al reembolsar | Base creada antes de existir `REFUNDED` | Recrear el `CHECK` ([05 §4](05-modelo-de-datos.md#4-gestión-del-esquema)) |
| El checkout en el teléfono no carga | Se usó `localhost` en la URL | Usar la IP LAN del equipo que ejecuta CPS |
