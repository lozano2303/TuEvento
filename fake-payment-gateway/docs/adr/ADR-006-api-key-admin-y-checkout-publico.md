# ADR-006 — API key en `/admin` y endpoints públicos dedicados para el checkout

**Estado:** Aceptado · **Fecha:** 2026-10-24 · **Modifica:** [ADR-004](ADR-004-checkout-hospedado.md)

## Contexto

En la primera versión, `/admin/**` no tenía autenticación y el checkout (`pay.html`) lo usaba para decidir el desenlace. Cualquiera que alcanzara CPS podía listar todos los pagos o reembolsar un pago ajeno. No se podía cerrar `/admin` sin romper el checkout.

## Decisión

1. **Separar dos superficies con dos modelos de confianza:**
   - `/public/checkout/{paymentId}/{approve|decline|fail|cancel}`: acciones del **pagador**. Se autorizan por posesión del `paymentId` (UUID v4, 122 bits aleatorios), igual que la página de pago de cualquier pasarela, que es anónima.
   - `/admin/**`: acciones del **comercio** y de QA (listar, reembolsar, cancelar, forzar desenlaces). Exigen la cabecera `X-Api-Key`.
2. **El reembolso no está en el checkout.** Un pagador no puede reembolsarse a sí mismo; el reembolso es decisión del comercio.
3. **Interceptor de Spring MVC** (`AdminApiKeyInterceptor`) registrado con el patrón `/admin/**`, en lugar de Spring Security: una sola regla no justifica la dependencia. Se descartó un filtro de servlet porque compararía el `requestURI` crudo, mientras Spring enruta sobre la ruta decodificada y sin `;…`: `/%61dmin/...` o `/admin;x=1/...` evadían la clave. El interceptor se evalúa sobre la misma ruta normalizada que el enrutamiento. Comparación con `MessageDigest.isEqual` (tiempo constante). Las preflight `OPTIONS` pasan sin clave.
4. **Fallar de forma segura:** la aplicación no arranca con la clave vacía y registra una advertencia si usa el valor de desarrollo por defecto.

## Alternativas consideradas

- **Token de checkout de un solo uso firmado:** más fuerte, pero obliga al cliente a manejar un segundo identificador. El `paymentId` aleatorio ya cumple la función de capacidad. Se reconsiderará con multi-comercio.
- **Spring Security con HTTP Basic:** funcional, pero agrega una cadena de filtros, CSRF y configuración que no aportan para una sola regla.

## Consecuencias

- ➕ `/admin` se puede exponer junto al checkout sin riesgo de que terceros reembolsen o enumeren pagos.
- ➕ El sistema cliente ejercita autenticación por API key, como hará con un proveedor real.
- ➖ El sistema cliente debe configurar una variable más (`FAKE_GATEWAY_API_KEY` en TuEvento).
- ➖ Quien conozca un `paymentId` puede decidir su desenlace mientras esté en `PENDING`. Es aceptable en un sandbox y equivale al modelo de las páginas de pago reales.
