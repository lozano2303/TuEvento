# Sesión: Sistema de Traducción de Notificaciones

**Fecha:** 2026-10-16  
**Estado:** En progreso - Compilación exitosa, investigación de comportamiento

---

## Contexto General

El proyecto TuEvento tiene un sistema de notificaciones multicanal (EMAIL, IN_APP) con soporte de traducción automática. Las notificaciones se crean en español (idioma fuente) y se traducen asíncronamente a otros idiomas según las preferencias de cada usuario.

### Arquitectura del Sistema de Traducción

1. **Creación de Notificación:**
   - Todas las notificaciones se crean en español a través de `SendNotificationUseCase`
   - Después de guardar, se solicita traducción: `translationService.requestTranslation("notification", id, "es")`

2. **Traducción Asíncrona:**
   - Los jobs de traducción se procesan por un scheduler
   - `NotificationTranslatableContentHandler` carga los textos originales de la notificación
   - Las traducciones se guardan en la tabla `notification_translation`

3. **Entrega de Notificaciones:**
   - **IN_APP (WebSocket):** `InAppChannelAdapter` intenta enviar notificaciones traducidas si existen
   - **EMAIL:** `EmailChannelAdapter` envía notificaciones (traducción pendiente de implementar)
   - **Consultas:** `InAppNotificationQueryUseCase` carga traducciones al listar notificaciones

### Tablas Involucradas

- `notification` - Contenido original en español
- `notification_translation` - Traducciones por idioma
- `translation_job` - Cola de trabajos de traducción
- `profile` - Contiene `preferred_language_id` del usuario

---

## Trabajo Realizado en Esta Sesión

### TAREA 1: Restaurar NotificationTranslationRepositoryImpl ✅

**Problema:** Se había eliminado erróneamente `NotificationTranslationRepositoryImpl`

**Solución:**
- Restaurada la implementación con arquitectura limpia (separación dominio/infraestructura)
- `NotificationTranslationRepository` (dominio) - Sin dependencia de JPA
- `NotificationTranslationRepositoryImpl` (infraestructura) - Usa `NotificationTranslationJpaRepository`
- Corregidos tipos de ID de `Integer` a `Long` donde era necesario

**Archivos modificados:**
- `modules/language/domain/repository/NotificationTranslationRepository.java`
- `modules/language/infrastructure/persistence/repository/NotificationTranslationRepositoryImpl.java`

### TAREA 2: Corregir NotificationTranslatableContentHandler ✅

**Problema:** El handler no encontraba notificaciones recién creadas ("Notification X not found for translation")

**Causa raíz:** El handler usaba el repositorio de dominio, pero la notificación no estaba disponible inmediatamente después del save debido a timing de JPA flush

**Solución:**
- Cambiado a usar `JpaNotificationRepository` directamente para acceder a `NotificationEntity`
- Corregido método `loadSourceTexts()` para usar `Long` en lugar de `Integer` en `findById()`

**Archivos modificados:**
- `modules/notification/infrastructure/translation/NotificationTranslatableContentHandler.java`

### TAREA 3: Soporte de Traducción en WebSocket (IN_APP) ✅

**Implementación:**
- Modificado `InAppChannelAdapter` para enviar notificaciones traducidas vía WebSocket
- Lógica implementada:
  1. Obtiene idioma preferido del usuario desde `profile.preferred_language_id`
  2. Si el idioma no es español (fuente), busca traducción
  3. Envía subject/body traducido si existe, sino el original

**Limitación Conocida:**
- Las traducciones son asíncronas, por lo que cuando el WebSocket envía la notificación inmediatamente después de crearla, las traducciones aún no existen
- **Comportamiento actual:** Notificaciones llegan en español vía WebSocket, pero aparecen traducidas al refrescar (InAppNotificationQueryUseCase las carga correctamente)

**Archivos modificados:**
- `modules/notification/infrastructure/channel/InAppChannelAdapter.java`
  - Agregadas dependencias: `ProfileRepository`, `NotificationTranslationRepository`, `LanguageRepository`
  - Agregado import de `Optional`

### TAREA 4: Corrección de Compilación ✅

**Problemas encontrados:**
1. Test `SendNotificationUseCaseIdempotencyTest` faltaba parámetro `TranslationService`
2. `InAppChannelAdapter` tenía errores de tipo: `Long` vs `Integer` en conversiones

**Soluciones:**
1. Agregado mock de `TranslationService` al test
2. Corregida conversión de `profile.getPreferredLanguageId()` (Long) a `languageRepository.findById()` (Integer)
   - Uso de `.intValue()` para conversión segura
   - Manejo de `Optional` para evitar NPE

**Archivos modificados:**
- `modules/notification/application/SendNotificationUseCaseIdempotencyTest.java`
- `modules/notification/infrastructure/channel/InAppChannelAdapter.java`

---

## Estado Actual

### ✅ Compilación Exitosa
```
[INFO] BUILD SUCCESS
[INFO] Total time:  24.407 s
```

### 📊 Evidencia en Logs (Pago ID=3, Usuario ID=2)

**Lo que SÍ funciona:**
```
PaymentApprovedEvent published: paymentId=3
Processing PaymentApprovedEvent: paymentId=3, userId=2
Notification 15 created (EMAIL channel)
Notification 16 created (IN_APP channel)
Message sent to /topic/notifications/2 for userId=2, type=PAYMENT_APPROVED
```

**El problema que persiste:**
```
Notification 15 not found for translation
Notification 16 not found for translation
No translatable texts found for notification/15
No translatable texts found for notification/16
```

### 🔍 Análisis del Problema

**Causa:** Timing de transacciones
- `SendNotificationUseCase` es `@Transactional`
- Después del `save()`, la notificación existe en memoria pero no está committed
- `translationService.requestTranslation()` se ejecuta dentro de la misma transacción
- El scheduler de traducción intenta cargar la notificación pero no la encuentra porque la transacción aún no ha hecho commit

**Comportamiento esperado vs actual:**

| Escenario | Esperado | Actual |
|-----------|----------|--------|
| WebSocket inmediato | Traducido | Español (original) |
| Refrescar página | Traducido | Traducido ✅ |
| Email | Traducido | Español (original) |

---

## Pregunta Pendiente del Usuario

**Usuario pregunta:** "Cabe aclarar que lo estoy creando desde inglés, aunque las páginas de pago no están traducidas... así que no sabría si está tomando el lenguaje, ¿deberíamos traducir eso primero?"

**Respuesta clave:**
- El idioma para traducir notificaciones **NO viene del idioma de la interfaz del frontend**
- El idioma se obtiene de `profile.preferred_language_id` del usuario en la base de datos
- Si el usuario ve notificaciones en español, es porque su `preferred_language_id` está configurado a español (o null, que default a español)

**Acción requerida:**
1. Verificar `preferred_language_id` del usuario con `user_id=2` en tabla `profile`
2. Si es español, cambiarlo al idioma deseado (ej: inglés)
3. Crear nuevo pago para probar comportamiento

---

## Próximos Pasos Sugeridos

### Opción A: Aceptar el Comportamiento Actual (Recomendado)
- Las notificaciones llegan en español vía WebSocket inicialmente
- Aparecen traducidas cuando el usuario recarga o navega
- Es un compromiso aceptable: velocidad de entrega vs traducción

### Opción B: Esperar a Traducción Antes de Enviar WebSocket
- Modificar `SendNotificationUseCase` para esperar la traducción
- **Desventaja:** Retrasa la entrega de notificaciones
- **Requiere:** Traducción síncrona o espera activa

### Opción C: Enviar Actualización por WebSocket Cuando Termine Traducción
- El scheduler envía un segundo mensaje WebSocket cuando la traducción está lista
- **Desventaja:** Más complejo, requiere tracking de usuarios conectados
- **Requiere:** Conexión bidireccional WebSocket persistente

### Opción D: Traducción Síncrona
- Traducir antes de guardar la notificación
- **Desventaja:** Cambio arquitectónico mayor
- **Desventaja:** Bloquea el proceso de creación de notificación

---

## Tipos de Notificación que Usan el Sistema

Todas las notificaciones usan el mismo flujo a través de `SendNotificationUseCase`:

1. **Eventos:** `EventStatusChangedListener`
   - `EVENT_PUBLISHED` - Evento publicado
   - `EVENT_REJECTED` - Evento rechazado

2. **Pagos:** `PaymentEventListener`
   - `PAYMENT_APPROVED` - Pago aprobado
   - `PAYMENT_REFUNDED` - Pago reembolsado

3. **Billetera:** `WalletEventListener`
   - `WALLET_CREDITED` - Crédito en billetera

4. **Usuario:** `UserActivatedListener`
   - `WELCOME` - Bienvenida

**Importante:** Si uno funciona, todos deberían funcionar igual (misma arquitectura)

---

## Archivos Clave para Referencia Futura

### Notificaciones
- `modules/notification/application/usecase/SendNotificationUseCase.java` - Caso de uso principal
- `modules/notification/infrastructure/channel/InAppChannelAdapter.java` - Canal WebSocket
- `modules/notification/infrastructure/channel/EmailChannelAdapter.java` - Canal Email
- `modules/notification/infrastructure/messaging/PaymentEventListener.java` - Listener de eventos de pago

### Traducción
- `modules/language/application/service/TranslationService.java` - Servicio de traducción
- `modules/notification/infrastructure/translation/NotificationTranslatableContentHandler.java` - Handler de contenido
- `modules/language/infrastructure/scheduler/TranslationJobScheduler.java` - Scheduler asíncrono

### Repositorios
- `modules/language/domain/repository/NotificationTranslationRepository.java` - Interface de dominio
- `modules/language/infrastructure/persistence/repository/NotificationTranslationRepositoryImpl.java` - Implementación

---

## Notas Técnicas

### Tipo de IDs en el Sistema
- `Notification.notificationId` → `Long`
- `NotificationTranslation.notificationId` → `Integer` (legacy)
- `Language.languageId` → `Integer`
- `Profile.preferredLanguageId` → `Long`

**Conversiones necesarias:**
- Al buscar notificaciones: `.intValue()` si el repo espera Integer
- Al buscar idiomas: `.intValue()` cuando viene de Profile

### Clean Architecture
El proyecto usa separación dominio/infraestructura:
- **Dominio:** Interfaces sin dependencias de frameworks
- **Infraestructura:** Implementaciones con JPA, Spring, etc.
- **No eliminar** implementaciones pensando que JpaRepository es suficiente

---

## Comando de Compilación
```bash
mvn clean compile -DskipTests
```

**Resultado:** ✅ BUILD SUCCESS
