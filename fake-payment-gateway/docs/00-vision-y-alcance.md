# 00 — Visión y alcance

**Producto:** CapySoft Payment Sandbox (CPS) · **Versión del producto:** 1.0.0 · **Versión del documento:** 1.0 · **Fecha:** 2026-10-24

---

## 1. Problema

Cualquier plataforma que cobra en línea depende de una pasarela de pagos externa. Desarrollar y probar contra esa pasarela trae problemas recurrentes:

1. **Dependencia de terceros en desarrollo.** Los entornos de prueba de los proveedores exigen registro comercial, llaves de API, aprobación de cuenta y a veces documentación legal antes de poder hacer la primera transacción de prueba.
2. **Escenarios difíciles de reproducir.** Provocar a voluntad un rechazo, una falla técnica, una cancelación o un reembolso en un sandbox de terceros suele requerir tarjetas mágicas, montos especiales o pasos manuales poco documentados.
3. **Webhooks inalcanzables.** Las pasarelas reales notifican por *webhook* a una URL pública; en desarrollo local (`localhost`) eso obliga a usar túneles (ngrok, etc.) y complica las pruebas.
4. **Acoplamiento prematuro.** Si el código de negocio se escribe directamente contra el SDK de un proveedor, cambiar de proveedor después es costoso.
5. **Costo y límites.** Algunos sandboxes tienen cuotas, caducidad de credenciales o no funcionan sin conexión.

## 2. Propuesta de valor

CPS es una **pasarela de pagos sandbox autocontenida** que CapySoft desarrolla y opera. Ofrece a los equipos de desarrollo, QA y demostración:

| Capacidad | Beneficio |
|---|---|
| API REST de creación y consulta de pagos | El sistema cliente se integra igual que con una pasarela real |
| Checkout hospedado (`/pay.html?id=...`) | El usuario final vive el flujo de redirección/popup de pago real, en web y en móvil |
| Control explícito del desenlace (aprobar, rechazar, fallar, cancelar, reembolsar) | Cualquier escenario de negocio es reproducible en segundos, sin tarjetas mágicas |
| Máquina de estados con reglas de dominio | Las transiciones ilegales se rechazan igual que en producción (no se puede reembolsar un pago rechazado) |
| Webhooks firmados HMAC-SHA256 con reintentos y bitácora | El sistema cliente implementa y prueba su verificación de firma, idempotencia y manejo de reintentos reales |
| Persistencia propia en PostgreSQL | Las transacciones sobreviven reinicios y se pueden auditar |
| Despliegue como contenedor independiente | Funciona offline, en local, en CI o en la nube, sin cuentas de terceros |

## 3. Sandbox, no mock

Este punto es central para entender qué es CPS.

### 3.1 Definiciones

- **Mock / stub:** objeto o función *dentro del proceso bajo prueba* que sustituye a una dependencia y devuelve respuestas predefinidas. No tiene estado propio duradero, no se despliega, no tiene interfaz de red, no aplica reglas de negocio. Ejemplo típico: `when(gateway.charge(any())).thenReturn(OK)` en un test unitario.
- **Sandbox:** **sistema independiente** que reproduce el comportamiento de un servicio real en un entorno aislado. Se despliega, tiene su propia persistencia, expone el mismo tipo de contrato que el servicio real y aplica sus reglas. Es lo que ofrecen Stripe (*test mode*), Wompi (*sandbox*), PayPal (*sandbox*) o Mercado Pago (*credenciales de prueba*).

### 3.2 Comparación objetiva

| Criterio | Mock | CPS | Evidencia en CPS |
|---|---|---|---|
| Proceso propio | No | **Sí** | Aplicación Spring Boot autónoma (`FakePaymentGatewayApplication`), puerto 4001 |
| Base de datos propia | No | **Sí** | PostgreSQL dedicado (`fake_gateway_db`), tablas `payments` y `webhook_delivery_logs` |
| Contrato de red (HTTP) | No | **Sí** | API `/public/payments`, `/public/checkout`, `/admin/payments`, ver [03](03-referencia-api.md) |
| Reglas de dominio | No | **Sí** | Máquina de estados en `PaymentStatus` + `Payment` (agregado DDD), `InvalidStatusTransitionException` |
| Comunicación asíncrona saliente | No | **Sí** | Webhooks HTTP POST firmados, 3 intentos con *backoff* en un pool asíncrono, ver [04](04-webhooks.md) |
| Seguridad de mensajes | No | **Sí** | Firma HMAC-SHA256 en cabecera `X-Signature` con secreto compartido |
| Control de acceso | No | **Sí** | `/admin/**` protegido con API key (`X-Api-Key`), comparación en tiempo constante |
| Pruebas automatizadas propias | No aplica | **Sí** | 52 pruebas (dominio, casos de uso, firma, reintentos, seguridad, contrato HTTP) con `./mvnw test` |
| Auditoría | No | **Sí** | Cada intento de entrega queda en `webhook_delivery_logs` (intento, HTTP status, error, fecha) |
| Interfaz de usuario | No | **Sí** | Checkout hospedado `pay.html` con *polling*, integración con popup web y WebView móvil |
| Despliegue independiente | No | **Sí** | `Dockerfile` multi-etapa, servicio propio en `docker-compose.yml` |
| Ciclo de vida propio | No | **Sí** | Versionado semántico (1.0.0), `CHANGELOG.md`, esta documentación |
| Consumible por cualquier cliente | No | **Sí** | El contrato no tiene nada específico de TuEvento: cualquier sistema que hable HTTP puede integrarse |

### 3.3 Cómo se integra la plataforma TuEvento

TuEvento **no conoce** a CPS en su lógica de negocio. El módulo de pagos de TuEvento define un puerto (`PaymentGatewayPort`) y CPS es solo uno de sus adaptadores (`FakePaymentGatewayAdapter`), que se activa con `payment.gateway=fake`. Sustituir CPS por un proveedor productivo es escribir otro adaptador y cambiar una propiedad: el resto de la plataforma no se entera. Esto es justamente lo que hace a CPS un sandbox de proveedor y no un artificio de prueba: está del **otro lado** de una frontera de integración real.

### 3.4 Sobre el nombre técnico

El artefacto Maven conserva el identificador histórico `fake-payment-gateway`. "Fake" describe que **el dinero no es real**, no que el sistema sea simulado a nivel de software: el software es real, persistente y desplegable. El nombre comercial del producto es **CapySoft Payment Sandbox**.

## 4. Posicionamiento

> **Para** equipos de desarrollo y QA **que** necesitan integrar y probar cobros en línea sin depender de un proveedor externo, **CapySoft Payment Sandbox** es una pasarela de pagos de prueba autocontenida **que** reproduce el ciclo de vida completo de una transacción con webhooks firmados y control total del desenlace. **A diferencia de** los sandboxes de proveedores comerciales, **CPS** no requiere registro, funciona sin conexión a internet, se despliega con un solo contenedor y permite forzar cualquier escenario con un clic o una llamada HTTP.

## 5. Interesados (stakeholders)

| Interesado | Interés |
|---|---|
| CapySoft (fabricante) | Producto reutilizable entre proyectos; reducción de costo de integración |
| Equipo de desarrollo de TuEvento (cliente interno) | Integrar el flujo de compra de boletas sin bloqueos externos |
| QA | Reproducir determinísticamente aprobaciones, rechazos, fallas, cancelaciones y reembolsos |
| Usuario de demostración / jurado / cliente final | Ver un flujo de pago completo y creíble en web y móvil |
| DevOps | Un servicio liviano, contenedorizado y configurable por variables de entorno |

## 6. Alcance

### 6.1 Dentro del alcance (v1.0.0)

- Creación y consulta de pagos con referencia externa, monto, moneda y método (`QR`, `CARD`, `PSE`).
- Checkout hospedado con actualización automática del estado.
- Transiciones: procesar, aprobar, rechazar, fallar, cancelar, reembolsar; validación de transiciones.
- Notificación por webhook de cada cambio de estado, con firma, reintentos y auditoría.
- Listado administrativo de pagos.
- Autenticación por API key (`X-Api-Key`) en la superficie administrativa.
- Despliegue con Docker; configuración por variables de entorno.

### 6.2 Fuera del alcance

- Movimiento de dinero real, conexión con redes de tarjetas, bancos o PSE reales.
- Captura o almacenamiento de datos de tarjeta (no aplica PCI-DSS porque no existen datos de tarjeta).
- Multi-comercio (un despliegue atiende a un único sistema cliente / una URL de webhook).
- Reembolsos parciales, pagos recurrentes, preautorizaciones, contracargos.

## 7. Supuestos y restricciones

- CPS **nunca** debe usarse para cobrar dinero real ni presentarse al usuario final como un medio de pago real.
- El sistema cliente comparte con CPS un secreto (`WEBHOOK_SECRET`) por un canal seguro.
- El sistema cliente expone un endpoint HTTP alcanzable desde CPS para recibir webhooks.

## 8. Métricas de éxito

| Métrica | Objetivo |
|---|---|
| Tiempo para que un equipo nuevo haga su primer pago de prueba | < 15 minutos siguiendo la [guía de integración](06-guia-de-integracion.md) |
| Escenarios de negocio reproducibles sin intervención externa | 100 % de los estados definidos |
| Cambios necesarios en el sistema cliente para pasar a un proveedor real | Solo un nuevo adaptador + configuración |
