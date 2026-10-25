# 11 — Glosario

| Término | Definición |
|---|---|
| **Adaptador** | Implementación concreta de un puerto (p. ej. `PaymentRepositoryAdapter` implementa `PaymentRepository` con JPA). |
| **Agregado (aggregate root)** | Entidad de dominio que protege sus invariantes y es la única puerta de entrada para modificarlas. En CPS: `Payment`. |
| **Backoff** | Espera creciente entre reintentos para no saturar al receptor. |
| **Checkout hospedado** | Página de pago servida por la pasarela, no por el sistema cliente. En CPS: `/pay.html`. |
| **CPS** | CapySoft Payment Sandbox, este producto. |
| **Estado terminal** | Estado desde el cual no se permite ninguna transición. |
| **Evento de dominio** | Hecho de negocio ocurrido, inmutable. En CPS: `PaymentStatusChanged`. |
| **`eventId`** | UUID único por transición, usado como clave de idempotencia por el cliente. |
| **`externalReference`** | Identificador del pago u orden en el sistema cliente. |
| **HMAC-SHA256** | Código de autenticación de mensajes basado en SHA-256 y una clave secreta (RFC 2104). |
| **Idempotencia** | Propiedad por la cual procesar el mismo mensaje varias veces produce el mismo efecto que procesarlo una vez. |
| **Mock** | Sustituto en proceso de una dependencia, usado en pruebas unitarias, sin estado ni despliegue propio. CPS **no** es un mock. |
| **Operador** | Persona que decide el desenlace de un pago desde el checkout. |
| **`paymentId`** | Identificador del pago asignado por CPS, con formato `fake_<UUID>`. |
| **Pasarela de pagos** | Servicio que intermedia entre un comercio y las redes de pago para autorizar cobros. |
| **Puerto** | Interfaz que define lo que el núcleo necesita del exterior (salida) o lo que ofrece (entrada). |
| **PSE** | Pagos Seguros en Línea: débito bancario en línea en Colombia. En CPS es solo un valor del enum `PaymentMethod`. |
| **QR** | Pago iniciado escaneando un código QR. En CPS es solo un valor del enum. |
| **Reconciliación** | Proceso de comparar el estado de las órdenes del cliente con el de la pasarela para corregir diferencias. |
| **Reembolso** | Devolución del monto de un pago aprobado. En CPS, siempre total. |
| **Sandbox** | Sistema independiente que reproduce el comportamiento de un servicio real en un entorno aislado, sin efectos reales. |
| **Sistema cliente** | Aplicación que consume CPS. |
| **Value object** | Objeto definido por sus valores, inmutable y sin identidad. En CPS: `Money`. |
| **Webhook** | Notificación HTTP que un sistema envía a otro cuando ocurre un evento. |
