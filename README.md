# Tu Evento

**Tu Evento** is a web and mobile platform developed by **CapySoft** for the creation, administration, and operation of events. It provides organizers with tools to design venue layouts, configure seating, manage reservations, and process payments securely. Attendees can browse events, purchase tickets, receive push notifications, and validate entry via QR code.

The backend is currently the active component under development. Frontend (web and mobile) will be integrated progressively.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Modules](#modules)
- [Database Model](#database-model)
- [Functional Requirements](#functional-requirements)
- [Non-Functional Requirements](#non-functional-requirements)
- [Getting Started](#getting-started)
- [Team](#team)

---

## Overview

Tu Evento targets two primary user types:

- **Organizers** — Create and publish events, design seating layouts, manage sections and pricing, and track attendance (web only).
- **Attendees** — Discover events, purchase tickets, select seats, receive notifications, and check in via QR code (web and mobile).

Additional roles include **anonymous users** (browse-only on web) and **administrators** (system-level management and organizer approval).

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 23, Spring Boot |
| Persistence | Spring Data JPA |
| Database | PostgreSQL 17 |
| Authentication | JWT + Refresh Token |
| Payment Gateway | Wompi |
| Notifications | SMTP (email), Push notifications |
| API Style | RESTful (JSON over HTTP) |
| Mobile | Android 11+, iOS (functional) |
| Web | Google Chrome, Microsoft Edge |

---

## Architecture

The system is built on a modular **N-Layer architecture with Domain-Driven Design (DDD)**, oriented toward microservices. Key architectural decisions:

- Authentication is handled via **JWT** with a **Refresh Token** system, ensuring secure and auditable session management.
- All client-server communication is performed over **HTTP** with JSON as the data exchange format.
- The **API Gateway** provides routing and access control per role.
- Venue layout and event configuration are **web-exclusive**; mobile clients focus on browsing, purchasing, and QR validation.

---

## Modules

The backend is organized into the following functional modules:

### Security
Handles user authentication, session management, and authorization. Includes JWT issuance, Refresh Token rotation, OAuth social login, account lockout, password recovery, and role/permission management.

### Profile
Manages user profile data, city and department references, activity logs, purchase history, and user preferences (language, theme, notifications).

### Event
Covers the full event lifecycle: creation, editing, publication, cancellation, and completion. Includes media management, layout storage (JSONB), rating and comment system with threaded replies, and event status audit logs.

### Category
Provides hierarchical categorization for events, supporting parent-child category trees and event-category associations.

### Geolocation
Models the geographic structure of sites (venues), linked to cities and departments. Supports latitude/longitude for physical location mapping.

### Section
Defines event sections (`event_section`) linked to section types, including capacity, available seats, price, and active status. Enforces integrity constraints on capacity and pricing.

### Seat
Manages individual seat records within blocks and sections. Tracks seat type (regular, courtesy), status (available, reserved, sold, courtesy), and logs all status transitions with reason tracking.

### Ticket
Handles ticket issuance per order, including QR code generation, expiration dates, check-in validation, and full status lifecycle logging.

### Payment
Processes orders and payments via the Wompi gateway. Tracks payment methods (card, PSE, Nequi, cash), transaction statuses, refund requests, and webhook event logs.

### Wallet
Provides a digital wallet per user with support for recharge, payment, withdrawal, and refund transaction types. Includes wallet summary tracking and references linking transactions to their originating entities.

### Notification
Delivers notifications through configurable channels (email, push, etc.) with support for typed notification categories. Tracks delivery status (Pending, Sent, Delivered, Failed) per user recipient.

### Theme
Allows users to select and customize interface themes (light, dark, purple, blue). Maintains per-user theme assignments, property-level customizations, and change logs.

### Language / Internationalization
Supports multilingual content (ES, EN, PT, JA) through a translation layer applied across entities: events, categories, channels, notifications, section types, profiles, and more.

---

## Database Model

The database is designed around the modules above. Each module has its own entity group with clear foreign key relationships. Below is a summary of the main tables per module:

| Module | Core Tables |
|---|---|
| Security | `user`, `role`, `permission`, `auth_session`, `refresh_token`, `account_lockout`, `account_activation`, `oauth_account`, `recover_password`, `password_history`, `login_credentials`, `organizer_petition` |
| Profile | `profile`, `preferences`, `activity_log`, `profile_log`, `purchase_history`, `wallet`, `wallet_transaction`, `wallet_summary` |
| Event | `event`, `event_media`, `event_media_log`, `event_layout`, `event_rating`, `event_comment_reply`, `event_status_log` |
| Category | `category`, `category_event` |
| Geolocation | `department`, `city`, `site` |
| Section | `event_section`, `section_type` |
| Seat | `seat`, `seat_block`, `seat_log` |
| Ticket | `ticket`, `ticket_log`, `ticket_checkin`, `seat_ticket` |
| Payment | `order`, `payment`, `payment_log`, `refund`, `transaction_webhook` |
| Wallet | `wallet`, `wallet_transaction`, `wallet_reference` |
| Notification | `notification`, `notification_type`, `notification_user`, `channel` |
| Theme | `theme`, `user_theme`, `theme_customization`, `theme_log` |
| Language | `language`, `*_translation` (one per translatable entity) |

> Key constraints applied across the model include `CHECK (finish_date > start_date)`, `UNIQUE(event_name, start_date, site_id)`, and role enforcement (`user.role = "ORGANIZER"`) where applicable.

---

## Functional Requirements

| ID | Requirement |
|---|---|
| RF1 | User registration |
| RF2 | Social authentication (OAuth) |
| RF3 | User login |
| RF4 | Profile management and personalization |
| RF5 | Password recovery |
| RF6 | Session logout |
| RF7 | Permanent account deletion |
| RF8 | System administration module |
| RF9 | Event structure editing and layout design |
| RF10 | Seat reservation |
| RF11 | QR code generation and validation per reservation |
| RF12 | iOS functional support |
| RF13 | Multilanguage support (ES / EN / PT / FR) |
| RF14 | Role-based dashboard (attendee, organizer, admin) |
| RF15 | Refresh Token session system |
| RF16 | Push notification system |
| RF17 | Wompi payment gateway integration |
| RF18 | Ticket purchase from mobile application |
| RF19 | Event layout and management from web |
| RF20 | Microservices-oriented architecture |
| RF21 | Backend migration to N-Layer architecture with DDD |
| RF22 | Interface theme system (light, dark, purple, blue) |
| RF23 | Event filters and search |
| RF24 | QR code reader for attendance validation |
| RF25 | Digital wallet system |

---

## Non-Functional Requirements

| ID | Requirement |
|---|---|
| RNF1 | Clear and descriptive error messages |
| RNF2 | Responsive design across web and mobile |
| RNF3 | Secure authentication and data protection |
| RNF4 | Usability — intuitive and accessible interface |

**Platform constraints:**
- Internet connection is required for all system operations.
- Web: compatible with Google Chrome and Microsoft Edge on Windows.
- Mobile: Android 11 or higher required; iOS support is functional per defined requirements.
- Venue layout design is available exclusively on the web platform.
- Mobile clients support event browsing, seat selection, ticket purchase, and QR scanning.

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.9+
- PostgreSQL 17

### Setup

```bash
# Clone the repository
git clone https://github.com/your-org/tu-evento.git
cd tu-evento

# Configure your database connection in application.properties
# spring.datasource.url=jdbc:postgresql://localhost:5432/tuevento
# spring.datasource.username=your_user
# spring.datasource.password=your_password

# Build and run
mvn clean install
mvn spring-boot:run
```

---

## Team

| Name | Role | Contact |
|---|---|---|
| Cristofer David Lozano Contreras | Desarrollador de Software | Cristofer_Lozano@soy.sena.edu.co |
| Jhampier Santos Ortiz | Desarrollador de Software | Jham_santos@soy.sena.edu.co |

---

*© 2025 — CapySoft*

---

# Tu Evento

**Tu Evento** es una plataforma web y móvil desarrollada por **CapySoft** para la creación, administración y operación de eventos. Proporciona a los organizadores herramientas para diseñar distribuciones de venues, configurar asientos, gestionar reservas y procesar pagos de forma segura. Los asistentes pueden explorar eventos, comprar entradas, recibir notificaciones push y validar su entrada mediante código QR.

El backend es actualmente el componente activo en desarrollo. El frontend (web y móvil) se integrará progresivamente.

---

## Tabla de Contenidos

- [Descripción General](#descripción-general)
- [Tecnologías](#tecnologías)
- [Arquitectura](#arquitectura-1)
- [Módulos](#módulos)
- [Modelo de Base de Datos](#modelo-de-base-de-datos)
- [Requisitos Funcionales](#requisitos-funcionales)
- [Requisitos No Funcionales](#requisitos-no-funcionales)
- [Cómo Empezar](#cómo-empezar)
- [Equipo](#equipo)

---

## Descripción General

Tu Evento está dirigido a dos tipos principales de usuarios:

- **Organizadores** — Crear y publicar eventos, diseñar distribuciones de asientos, gestionar secciones y precios, y controlar la asistencia (solo web).
- **Asistentes** — Descubrir eventos, comprar entradas, seleccionar asientos, recibir notificaciones y registrarse mediante código QR (web y móvil).

Los roles adicionales incluyen **usuarios anónimos** (solo navegación en web) y **administradores** (gestión a nivel de sistema y aprobación de organizadores).

---

## Tecnologías

| Capa | Tecnología |
|---|---|
| Backend | Java 23, Spring Boot |
| Persistencia | Spring Data JPA |
| Base de Datos | PostgreSQL 17 |
| Autenticación | JWT + Refresh Token |
| Pasarela de Pago | Wompi |
| Notificaciones | SMTP (email), Notificaciones push |
| Estilo de API | RESTful (JSON sobre HTTP) |
| Móvil | Android 11+, iOS (funcional) |
| Web | Google Chrome, Microsoft Edge |

---

## Arquitectura

El sistema está construido sobre una **arquitectura modular N-Layer con Domain-Driven Design (DDD)**, orientada a microservicios. Decisiones arquitectónicas clave:

- La autenticación se maneja mediante **JWT** con un sistema de **Refresh Token**, asegurando una gestión de sesiones segura y auditable.
- Toda la comunicación cliente-servidor se realiza sobre **HTTP** con JSON como formato de intercambio de datos.
- La **API Gateway** proporciona enrutamiento y control de acceso por rol.
- La distribución de venues y configuración de eventos son **exclusivas de web**; los clientes móviles se enfocan en navegación, compra y validación QR.

---

## Módulos

El backend está organizado en los siguientes módulos funcionales:

### Seguridad
Maneja la autenticación de usuarios, gestión de sesiones y autorización. Incluye emisión de JWT, rotación de Refresh Token, login social OAuth, bloqueo de cuentas, recuperación de contraseña, y gestión de roles/permisos.

### Perfil
Gestiona datos del perfil de usuario, referencias de ciudad y departamento, registros de actividad, historial de compras, y preferencias de usuario (idioma, tema, notificaciones).

### Evento
Cubre el ciclo de vida completo del evento: creación, edición, publicación, cancelación y completado. Incluye gestión de medios, almacenamiento de distribución (JSONB), sistema de calificación y comentarios con respuestas hilvanadas, y registros de auditoría de estado del evento.

### Categoría
Proporciona categorización jerárquica para eventos, soportando árboles de categorías padre-hijo y asociaciones evento-categoría.

### Geolocalización
Modela la estructura geográfica de los sitios (venues), vinculados a ciudades y departamentos. Soporta latitud/longitud para mapeo de ubicación física.

### Sección
Define secciones de eventos (`event_section` vinculadas a tipos de sección), incluyendo capacidad, asientos disponibles, precio y estado activo. Impone restricciones de integridad en capacidad y precios.

### Asiento
Gestiona registros individuales de asientos dentro de bloques y secciones. Rastrea tipo de asiento (regular, cortesía), estado (disponible, reservado, vendido, cortesía) y registra todas las transiciones de estado con seguimiento de motivos.

### Entrada
Maneja la emisión de entradas por orden, incluyendo generación de código QR, fechas de expiración, validación de check-in y registro completo del ciclo de vida del estado.

### Pago
Procesa órdenes y pagos mediante la pasarela Wompi. Rastrea métodos de pago (card, PSE, Nequi, efectivo), estados de transacción, solicitudes de reembolso y registros de eventos webhook.

### Billetera
Proporciona una billetera digital por usuario con soporte para recarga, pago, retiro y tipos de transacción de reembolso. Incluye seguimiento de resumen de billetera y referencias que vinculan transacciones con sus entidades originarias.

### Notificación
Entrega notificaciones a través de canales configurables (email, push, etc.) con soporte para categorías de notificación tipificadas. Rastrea estado de entrega (Pendiente, Enviado, Entregado, Fallido) por destinatario usuario.

### Tema
Permite a los usuarios seleccionar y personalizar temas de interfaz (light, dark, purple, blue). Mantiene asignaciones de tema por usuario, personalizaciones a nivel de propiedad y registros de cambios.

### Idioma / Internacionalización
Soporta contenido multilingüe (ES, EN, PT, JA) a través de una capa de traducción aplicada entre entidades: eventos, categorías, canales, notificaciones, tipos de sección, perfiles y más.

---

## Modelo de Base de Datos

La base de datos está diseñada alrededor de los módulos anteriores. Cada módulo tiene su propio grupo de entidades con relaciones claras de clave foránea. A continuación un resumen de las principales tablas por módulo:

| Módulo | Tablas Principales |
|---|---|
| Seguridad | `user`, `role`, `permission`, `auth_session`, `refresh_token`, `account_lockout`, `account_activation`, `oauth_account`, `recover_password`, `password_history`, `login_credentials`, `organizer_petition` |
| Perfil | `profile`, `preferences`, `activity_log`, `profile_log`, `purchase_history`, `wallet`, `wallet_transaction`, `wallet_summary` |
| Evento | `event`, `event_media`, `event_media_log`, `event_layout`, `event_rating`, `event_comment_reply`, `event_status_log` |
| Categoría | `category`, `category_event` |
| Geolocalización | `department`, `city`, `site` |
| Sección | `event_section`, `section_type` |
| Asiento | `seat`, `seat_block`, `seat_log` |
| Entrada | `ticket`, `ticket_log`, `ticket_checkin`, `seat_ticket` |
| Pago | `order`, `payment`, `payment_log`, `refund`, `transaction_webhook` |
| Billetera | `wallet`, `wallet_transaction`, `wallet_reference` |
| Notificación | `notification`, `notification_type`, `notification_user`, `channel` |
| Tema | `theme`, `user_theme`, `theme_customization`, `theme_log` |
| Idioma | `language`, `*_translation` (una por entidad traducible) |

> Las restricciones clave aplicadas en el modelo incluyen `CHECK (finish_date > start_date)`, `UNIQUE(event_name, start_date, site_id)` y aplicación de roles (`user.role = "ORGANIZER"`) donde corresponda.

---

## Requisitos Funcionales

| ID | Requisito |
|---|---|
| RF1 | Registro de usuario |
| RF2 | Autenticación social (OAuth) |
| RF3 | Inicio de sesión de usuario |
| RF4 | Gestión y personalización de perfil |
| RF5 | Recuperación de contraseña |
| RF6 | Cierre de sesión |
| RF7 | Eliminación permanente de cuenta |
| RF8 | Módulo de administración del sistema |
| RF9 | Edición de estructura de eventos y diseño de distribución |
| RF10 | Reserva de asientos |
| RF11 | Generación y validación de código QR por reserva |
| RF12 | Soporte funcional para iOS |
| RF13 | Soporte multilingüe (ES / EN / PT / FR) |
| RF14 | Panel basado en roles (asistente, organizador, admin) |
| RF15 | Sistema de sesión con Refresh Token |
| RF16 | Sistema de notificaciones push |
| RF17 | Integración con pasarela de pago Wompi |
| RF18 | Compra de entradas desde aplicación móvil |
| RF19 | Gestión y distribución de eventos desde web |
| RF20 | Arquitectura orientada a microservicios |
| RF21 | Migración del backend a arquitectura N-Layer con DDD |
| RF22 | Sistema de temas de interfaz (light, dark, purple, blue) |
| RF23 | Filtros y búsqueda de eventos |
| RF24 | Lector de código QR para validación de asistencia |
| RF25 | Sistema de billetera digital |

---

## Requisitos No Funcionales

| ID | Requisito |
|---|---|
| RNF1 | Mensajes de error claros y descriptivos |
| RNF2 | Diseño responsivo en web y móvil |
| RNF3 | Autenticación segura y protección de datos |
| RNF4 | Usabilidad — interfaz intuitiva y accesible |

**Restricciones de la plataforma:**
- Se requiere conexión a internet para todas las operaciones del sistema.
- Web: compatible con Google Chrome y Microsoft Edge en Windows.
- Móvil: Se requiere Android 11 o superior; el soporte para iOS es funcional según los requisitos definidos.
- El diseño de distribución de venues está disponible exclusivamente en la plataforma web.
- Los clientes móviles soportan navegación de eventos, selección de asientos, compra de entradas y escaneo QR.

---

## Cómo Empezar

### Prerrequisitos

- Java 17+
- Maven 3.9+
- PostgreSQL 17

### Configuración

```bash
# Clonar el repositorio
git clone https://github.com/your-org/tu-evento.git
cd tu-evento

# Configurar la conexión a la base de datos en application.properties
# spring.datasource.url=jdbc:postgresql://localhost:5432/tuevento
# spring.datasource.username=your_user
# spring.datasource.password=your_password

# Compilar y ejecutar
mvn clean install
mvn spring-boot:run
```

---

## Equipo

| Nombre | Rol | Contacto |
|---|---|---|
| Cristofer David Lozano Contreras | Desarrollador de Software | Cristofer_Lozano@soy.sena.edu.co |
| Jhampier Santos Ortiz | Desarrollador de Software | Jham_santos@soy.sena.edu.co |

---

*© 2025 — CapySoft*
