# CapySoft Payment Sandbox

Pasarela de pagos *sandbox* desarrollada por **CapySoft**. Reproduce el ciclo de vida completo de una transacción (creación, checkout hospedado, aprobación, rechazo, falla, cancelación y reembolso) con persistencia propia y webhooks firmados con HMAC-SHA256, para que los sistemas cliente se integren y prueben sus cobros sin depender de un proveedor externo ni mover dinero real.

> Artefacto: `com.capysoft:fake-payment-gateway:1.0.0` — "fake" se refiere al dinero, no al software.

## Inicio rápido

```bash
# desde la raíz del monorepo
docker compose up -d postgres-fake-gateway fake-payment-gateway

curl -X POST http://localhost:4001/public/payments -H "Content-Type: application/json" \
  -d '{"externalReference":"DEMO-1","amount":50000,"currency":"COP","paymentMethod":"CARD"}'

# abrir en el navegador el checkout del paymentId devuelto
http://localhost:4001/pay.html?id=fake_...
```

## Características

- API REST para crear, consultar, cancelar y reembolsar pagos.
- Checkout hospedado para web (popup) y móvil (WebView), con actualización automática del estado.
- Máquina de estados con reglas de dominio: las transiciones inválidas se rechazan.
- Webhooks firmados (`X-Signature`), con `eventId` para idempotencia, 3 intentos asíncronos con *timeouts* y bitácora de auditoría.
- Superficie administrativa protegida con API key (`X-Api-Key`); checkout con endpoints públicos propios.
- Errores de cliente con códigos 4xx y detalle por campo.
- 53 pruebas automatizadas: `./mvnw test` (no requieren base de datos).
- Base PostgreSQL dedicada; configuración 100 % por variables de entorno; imagen Docker multi-etapa.

## Stack

Java 21 · Spring Boot 3.5 · Spring Data JPA · PostgreSQL 17 · arquitectura hexagonal.

## Documentación

Toda la documentación del producto está en [`docs/`](docs/README.md): visión y alcance, SRS, arquitectura, referencia de la API, webhooks, modelo de datos, guía de integración, despliegue, plan de pruebas, seguridad, hoja de ruta, glosario y ADRs.

## Cambios

Ver [`CHANGELOG.md`](CHANGELOG.md).
