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
| Cristofer David Lozano Contreras | Backend Developer, Database Administrator | Cristofer_Lozano@soy.sena.edu.co |
| Jhampier Santos Ortiz | Frontend Developer, Documentation | Jham_santos@soy.sena.edu.co |

---

*© 2025 — CapySoft*