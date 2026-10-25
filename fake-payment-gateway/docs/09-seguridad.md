# 09 — Seguridad

**Producto:** CapySoft Payment Sandbox (CPS) 1.0.0 · **Versión del documento:** 1.1 · **Fecha:** 2026-10-24

---

## 1. Postura general

CPS es un entorno de **pruebas**: no procesa dinero ni datos de tarjeta, por lo que el impacto de un compromiso es limitado. Aun así se diseñó con los mismos controles de integridad de mensajes que una pasarela real, porque su objetivo es que el sistema cliente **implemente y ejercite** esos controles antes de llegar a producción.

## 2. Datos que maneja

| Dato | ¿Lo maneja CPS? |
|---|---|
| Número de tarjeta, CVV, fecha de vencimiento | **No** |
| Datos personales del pagador (nombre, documento, correo) | **No** |
| Referencia externa de la orden | Sí (texto opaco del cliente) |
| Monto y moneda | Sí |
| Secreto de webhook | Sí (variable de entorno, nunca en BD ni en logs) |

Al no existir datos de titular de tarjeta, PCI-DSS no aplica. Los integradores **no deben** enviar datos personales en `externalReference`.

## 3. Controles implementados

| Control | Descripción |
|---|---|
| Autenticidad e integridad de webhooks | HMAC-SHA256 del cuerpo con secreto compartido en `X-Signature` |
| Control de acceso administrativo | `X-Api-Key` obligatoria en `/admin/**`, comparada en tiempo constante. La clave no tiene valor por defecto (*fail-closed*): sin ella, vacía o con menos de 16 caracteres, CPS no arranca; advertencia si se usa la clave de desarrollo |
| Separación pagador / comercio | El checkout solo puede aprobar, rechazar, fallar o cancelar (`/public/checkout`); reembolsar y listar requieren la API key |
| Errores sin filtración | Los 500 responden `Unexpected error`; el detalle va solo al log |
| Resiliencia del HTTP saliente | *Timeouts* de 3 s / 5 s y reintentos fuera del hilo de la petición |
| Idempotencia | `eventId` único por transición, garantizado con `UNIQUE` en BD |
| Integridad del estado | Máquina de estados en el dominio + `CHECK` en BD |
| Validación de entrada | Bean Validation en `CreatePaymentRequest`; invariantes en `Money`; longitudes de columna |
| Secretos fuera del código | Configuración por entorno; `.env` en `.gitignore`; `.env.example` sin valores reales |
| Mínima superficie de datos | No se almacenan datos personales |
| Imagen mínima | Runtime JRE Alpine sin herramientas de compilación |

## 4. Modelo de amenazas (STRIDE)

| Amenaza | Escenario | Mitigación actual | Riesgo residual | Acción planificada |
|---|---|---|---|---|
| **S**uplantación | Un tercero envía webhooks falsos al cliente | Firma HMAC | Bajo, si el cliente verifica | — |
| **S**uplantación | Un tercero llama a `/admin/**` y reembolsa o lista pagos | API key en `X-Api-Key` (ADR-006) | Bajo | Rotación de claves por comercio en 2.0.0 |
| **S**uplantación | Un tercero decide el desenlace de un pago ajeno desde el checkout | El `paymentId` es un UUID v4 no enumerable; el listado requiere API key; solo aplica a pagos `PENDING` | Bajo | Token de checkout de un solo uso (2.0.0) |
| **T**ampering | Alteración del webhook en tránsito | HMAC; TLS en el despliegue | Bajo | — |
| **R**epudio | No saber si un webhook se envió | Bitácora `webhook_delivery_logs` | Bajo | Agregar `payment_id` a la bitácora (L-07) |
| **I**nformation disclosure | Enumerar pagos con `GET /admin/payments` | API key | Bajo | — |
| **I**nformation disclosure | Mensajes de excepción en respuestas 500 | Mensaje genérico; detalle solo en log | Bajo | — |
| **D**enial of service | URL de webhook lenta bloquea hilos | *Timeouts* + reintentos asíncronos | Bajo | *Outbox* persistente (L-13) |
| **D**enial of service | Creación masiva de pagos | Ninguna | Medio | *Rate limiting* en ingress |
| **E**levation of privilege | — | No hay roles | N/A | — |
| Repetición (*replay*) | Reenviar un webhook capturado | Idempotencia por `eventId` en el cliente | Bajo | Cabecera de *timestamp* firmada (L-10) |

## 5. Recomendaciones de despliegue

1. **Nunca** usar el valor por defecto de `WEBHOOK_SECRET`; generar uno de ≥ 32 bytes aleatorios por entorno.
2. Rotar el secreto coordinadamente con el cliente si se sospecha filtración.
3. Generar `ADMIN_API_KEY` aleatoria (≥ 32 bytes) por entorno y compartirla solo con el sistema cliente. Como defensa en profundidad, publicar solo `/pay.html` y `/public/**` en el ingress: el checkout ya no depende de `/admin`.
4. Terminar TLS en el ingress o en la plataforma (Render lo provee).
5. Usar un usuario de PostgreSQL dedicado con permisos solo sobre la base de CPS.
6. Mostrar en la UI del sistema cliente que el entorno es de pruebas, para que ningún usuario crea que realiza un pago real.

## 6. Recomendaciones para el sistema cliente

- Verificar la firma sobre el cuerpo crudo con comparación en tiempo constante.
- Guardar la API key de CPS como secreto (nunca en el frontend ni en el repositorio).
- Procesar webhooks de forma idempotente.
- No confiar en el `postMessage` del checkout ni en parámetros de URL para marcar una orden como pagada.
- Validar que `amount` y `currency` del webhook coinciden con la orden.
- Registrar los webhooks rechazados para detectar intentos de suplantación.

## 7. Reporte de vulnerabilidades

Reporte cualquier hallazgo al equipo de CapySoft por el canal interno del proyecto, sin abrir *issues* públicos con detalles explotables.
