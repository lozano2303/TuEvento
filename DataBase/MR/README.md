# Modelo Relacional (MR) — Tu Evento (Capysoft)

Este directorio contiene el Modelo Relacional derivado del MER del proyecto, dividido en los mismos 14 módulos funcionales del sistema, más una versión consolidada con el modelo completo.

## Estructura

```
MR/
├── DBML/     -> código fuente del modelo (formato DBML, editable en dbdiagram.io)
└── pdfs/     -> diagramas ya renderizados, uno por módulo
```

Cada archivo tiene el mismo nombre base en ambas carpetas (solo cambia la extensión), para poder ubicar rápido el PDF que corresponde a cada `.dbml`.

## Módulos

| Módulo | Archivo | Contenido |
|---|---|---|
| Seguridad | `modulo_seguridad` | Usuarios, autenticación, roles y permisos, activación de cuenta, recuperación de contraseña y sesiones (tokens de acceso y refresco). |
| Perfil | `modulo_perfil` | Perfil público del usuario, preferencias de idioma y tema, historial de actividad, historial de compras y resumen de billetera. |
| Geolocalización | `modulo_geolocalizacion` | Departamentos, ciudades y sitios (recintos) donde se realizan los eventos. |
| Eventos | `modulo_eventos` | Núcleo del sistema: eventos, su layout gráfico, calificaciones, comentarios y galería multimedia. |
| Categorías | `modulo_categorias` | Clasificación de eventos por categorías, con soporte de categorías jerárquicas (categoría padre). |
| Secciones | `modulo_secciones` | Secciones y tipos de sección dentro de un evento (zonas del recinto: platea, palco, general, etc.). |
| Sillas | `modulo_sillas` | Asientos individuales dentro de cada bloque y sección del evento, con su historial de cambios de estado. |
| Tickets | `modulo_tickets` | Boletos generados por evento, su relación con los asientos ocupados, el check-in de ingreso y su historial de estados. |
| Pagos | `modulo_pagos` | Órdenes de compra, pagos, reembolsos y webhooks recibidos de las pasarelas de pago externas. |
| Cartera | `modulo_cartera` | Billetera virtual del usuario, sus transacciones y la referencia a la entidad de origen de cada movimiento. |
| Notificaciones | `modulo_notificaciones` | Envío de notificaciones a usuarios a través de distintos canales (email, push, etc.) y su estado de entrega. |
| Temas | `modulo_temas` | Temas visuales disponibles y personalización de tema aplicada por cada usuario. |
| Idiomas | `modulo_idiomas` | Traducciones multilenguaje de las distintas entidades del sistema (eventos, categorías, notificaciones, etc.). |
| Almacenamiento | `modulo_almacenamiento` | Proveedores de almacenamiento, categorías de archivo y archivos subidos al sistema, con su bitácora de operaciones. |
| **Completo** | `mer_completo` | Vista consolidada de las 78 tablas y 117 relaciones del sistema, sin duplicados entre módulos. |

## Notas de diseño

Al pasar de MER a MR se aplicaron correcciones puntuales frente al diagrama original (documentadas también como `Note` dentro de cada tabla en el `.dbml` correspondiente):

- **PK compuestas mal formadas** (`category_event`, `user_theme`, `account_activation`, `module_translation`, `action_translation`): se dejó un id sustituto como PK única y las columnas que estaban dobladas como PK pasaron a una restricción `UNIQUE`.
- **`seat_ticket`** sí quedó con PK compuesta real (`seat_id + ticket_id`), por ser una tabla N:M genuina sin id sustituto en el diseño original.
- **Tipos de dato**: `String(n)` → `varchar(n)`, `TimesTamp` → `timestamp`, `ENUM` → tipos `Enum` de DBML con valores definidos (no venían especificados en el MER original; ajustar si el equipo ya maneja otro catálogo de valores).
- **Pendientes a decidir en equipo**: `order` es palabra reservada en SQL (usar comillas o renombrar a `orders` al implementar); `review` y `event_rating` parecen representar la misma entidad duplicada entre módulos; `purchase_history` y `wallet_summary` son tablas de solo lectura que podrían reemplazarse por una `VIEW` en vez de tabla física.

## Autores

Cristofer David Lozano Contreras · Jhampier Santos Ortiz
Análisis y Desarrollo de Software — Ficha 3145556
SENA, Centro de Formación (Neiva, Huila)
