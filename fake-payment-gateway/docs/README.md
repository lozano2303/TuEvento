# CapySoft Payment Sandbox — Documentación del producto

> **Artefacto técnico:** `com.capysoft:fake-payment-gateway:1.0.0`
> **Fabricante:** CapySoft
> **Tipo de producto:** pasarela de pagos *sandbox* (entorno de pruebas transaccional)
> **Estado:** 1.0.0 — en uso como pasarela por defecto de la plataforma TuEvento

CapySoft Payment Sandbox (en adelante **CPS** o "el Sandbox") es un microservicio desarrollado por CapySoft que reproduce el ciclo de vida completo de una pasarela de pagos real: creación de transacciones, checkout hospedado, máquina de estados con reglas de negocio, persistencia propia, notificaciones *webhook* firmadas con HMAC-SHA256, reintentos con *backoff* y bitácora de auditoría de entregas.

No es un *mock*. Un mock vive dentro del proceso que se prueba y devuelve respuestas fijas; CPS es un **sistema independiente**, con su propio proceso, base de datos, API pública, interfaz de usuario y contrato de integración, que se despliega y opera por separado y al que cualquier sistema cliente se conecta por HTTP, exactamente como lo haría con Wompi, Stripe o Mercado Pago en sus modos de prueba. La justificación completa está en [00 — Visión y alcance](00-vision-y-alcance.md#3-sandbox-no-mock).

## Mapa de la documentación

| # | Documento | Para quién | Contenido |
|---|---|---|---|
| 00 | [Visión y alcance](00-vision-y-alcance.md) | Todos | Problema, propuesta de valor, posicionamiento, diferencia sandbox vs mock, alcance |
| 01 | [Especificación de Requisitos de Software (SRS)](01-SRS.md) | Producto, QA, desarrollo | Requisitos funcionales y no funcionales, casos de uso, trazabilidad (estructura ISO/IEC/IEEE 29148) |
| 02 | [Documento de arquitectura (SAD)](02-arquitectura.md) | Desarrollo, arquitectura | Vistas C4, arquitectura hexagonal, flujos, decisiones |
| 03 | [Referencia de la API](03-referencia-api.md) | Integradores | Endpoints, esquemas, códigos de estado, ejemplos |
| 04 | [Especificación de webhooks](04-webhooks.md) | Integradores | Eventos, payload, firma, reintentos, idempotencia |
| 05 | [Modelo de datos](05-modelo-de-datos.md) | Desarrollo, DBA | Modelo de dominio, esquema relacional, máquina de estados |
| 06 | [Guía de integración](06-guia-de-integracion.md) | Integradores | Paso a paso para conectar un sistema cliente, con el caso TuEvento como referencia |
| 07 | [Despliegue y operación](07-despliegue-y-operacion.md) | DevOps | Docker, variables de entorno, puertos, monitoreo, troubleshooting |
| 08 | [Plan de pruebas](08-plan-de-pruebas.md) | QA | Estrategia, casos de prueba, criterios de aceptación |
| 09 | [Seguridad](09-seguridad.md) | Seguridad, DevOps | Modelo de amenazas, controles, recomendaciones de endurecimiento |
| 10 | [Limitaciones conocidas y hoja de ruta](10-limitaciones-y-roadmap.md) | Producto, desarrollo | Deuda técnica identificada y versiones planificadas |
| 11 | [Glosario](11-glosario.md) | Todos | Terminología del dominio de pagos y del producto |
| ADR | [Registros de decisiones de arquitectura](adr/) | Arquitectura | Decisiones clave y su justificación |

## Lectura recomendada según el rol

- **Evaluador / jurado / cliente:** 00 → 01 (secciones 1–3) → 02 (sección 2) → 10.
- **Equipo que integra un sistema nuevo:** 06 → 03 → 04 → 09.
- **Equipo que mantiene CPS:** 02 → 05 → ADRs → 08 → 10.
- **Operaciones:** 07 → 09.

## Control de versiones del documento

| Versión | Fecha | Autor | Cambios |
|---|---|---|---|
| 1.0 | 2026-10-24 | CapySoft | Versión inicial del conjunto documental para la release 1.0.0 |
| 1.1 | 2026-10-24 | CapySoft | Actualización tras resolver L-01, L-02, L-03, L-04, L-09 y L-12; nuevo ADR-006 |
