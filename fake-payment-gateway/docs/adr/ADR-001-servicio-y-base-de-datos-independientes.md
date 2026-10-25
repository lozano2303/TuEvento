# ADR-001 — Servicio y base de datos independientes del sistema cliente

**Estado:** Aceptado · **Fecha:** 2026

## Contexto

TuEvento necesita cobrar boletas. Durante el desarrollo no se quiere depender de un proveedor externo. Había tres opciones:

1. Un *mock* dentro del backend de TuEvento (una clase que devuelve "aprobado").
2. Un módulo dentro del backend con tablas propias en la misma base de datos.
3. Un servicio independiente con su propia base de datos.

Con las opciones 1 y 2 no se ejercita la frontera de red real: no hay latencia, no hay fallas de conexión, no hay webhooks entrantes, no hay verificación de firma y la lógica de pagos termina mezclada con la de negocio. El código que llega a producción nunca habría sido probado contra un sistema externo.

## Decisión

CPS se construye como **servicio independiente** (proceso, puerto, imagen Docker y ciclo de versiones propios) con **base de datos PostgreSQL dedicada** (`fake_gateway_db`, contenedor `postgres-fake-gateway`). El sistema cliente se comunica con CPS exclusivamente por HTTP.

## Consecuencias

- ➕ El sistema cliente se integra con CPS de la misma forma que con una pasarela comercial; el adaptador y el endpoint de webhooks que se prueban son los que se usarán en producción.
- ➕ CPS es reutilizable por cualquier otro proyecto de CapySoft.
- ➕ Fallas de red, reintentos y firmas inválidas se pueden provocar y probar.
- ➖ Un contenedor y una base de datos más que operar en local.
- ➖ La configuración de red (URL de webhook, `host.docker.internal`) requiere atención.
