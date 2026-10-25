# ADR-004 — Checkout hospedado servido por la pasarela

**Estado:** Aceptado · **Fecha:** 2026

## Contexto

En una pasarela real el usuario sale de la aplicación del comercio (redirección, popup o WebView) hacia una página de la pasarela, y luego vuelve. Si el sandbox solo ofreciera endpoints, el flujo de usuario no podría probarse ni demostrarse.

## Decisión

- CPS sirve una página estática `pay.html` (HTML, CSS y JS sin dependencias) en `/pay.html?id={paymentId}`.
- La página consulta el pago, lo refresca cada 3 s y permite decidir el desenlace.
- Llama a la API usando `window.location.origin`, de modo que funciona al abrirse desde un teléfono por la IP de la red local.
- Si fue abierta como popup, informa a la ventana padre con `postMessage({type: 'GATEWAY_ACTION', action})` y se cierra.

## Consecuencias

- ➕ El flujo de compra completo se puede demostrar en web y móvil.
- ➕ Sin *build* de frontend: un solo artefacto JAR.
- ➕ El cliente aprende a no confiar en el navegador: la confirmación real llega por webhook.
- ➖ En la primera versión el checkout usaba endpoints `/admin`. **Superado por [ADR-006](ADR-006-api-key-admin-y-checkout-publico.md):** el checkout usa `/public/checkout` y `/admin` exige API key.
- ➖ La página está solo en español.
