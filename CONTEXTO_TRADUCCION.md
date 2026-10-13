# Contexto del Sistema de Traducción - Tu Evento

## Descripción General del Proyecto

**Tu Evento** es una plataforma de gestión de eventos con arquitectura hexagonal (DDD). El proyecto tiene:
- **Backend**: Spring Boot con Java 21, base de datos PostgreSQL
- **Frontend**: React con Vite
- **Servicios externos**: LibreTranslate para traducciones automáticas

---

## Sistema de Traducción Implementado

### Arquitectura

El sistema de traducción permite traducir automáticamente contenido de eventos (y otras entidades) a múltiples idiomas usando LibreTranslate.

**Componentes principales:**

1. **Tablas de traducción** (14 tablas creadas):
   - `event_translation`, `category_translation`, `theme_translation`, etc.
   - Cada tabla tiene: `translation_id` (INT), `entity_id`, `language_id` (INT), `translated_*` campos, `source`, `status`

2. **Translation Jobs**:
   - Tabla `translation_job` almacena trabajos de traducción pendientes/procesados
   - Campos: `job_id` (INT), `entity_type`, `entity_id`, `source_language_id`, `target_language_id`, `status` (ENUM), `attempts`, etc.

3. **Handlers**:
   - Interface `TranslatableContentHandler` define cómo traducir cada entidad
   - Implementados: `EventTranslatableContentHandler`, `CategoryTranslatableContentHandler`
   - Registrados en un registry con clave por tipo de entidad

4. **Procesador asíncrono**:
   - `TranslationProcessor` procesa jobs en thread pool separado
   - Usa `@Async("translationExecutor")` con configuración en `TranslationConfig`

5. **Flujo de trabajo**:
   ```
   Event creado → EventTranslationListener (@TransactionalEventListener AFTER_COMMIT)
               → TranslationService.requestTranslation()
               → Crea 3 jobs (en, fr, pt) con status=PENDING
               → TranslationProcessor.processTranslationJobs() (async)
               → Para cada job: claim → translate → save → mark completed
   ```

---

## Problemas Resueltos (Sesión Actual)

### Problema 1: Token Refresh para WebSocket
**Estado**: ✅ Resuelto
- Fixed `RefreshTokenJpaRepository` query para manejar tokens duplicados (filtrar revoked=false, ordenar por issuedAt DESC, LIMIT 1)
- Creado hook `useTokenRefresh` que refresca token cada 12 minutos
- `NotificationContext.reconnectWebSocket(newToken)` actualiza conexión WebSocket

### Problema 2: Errores de Compilación en Módulo de Traducción
**Estado**: ✅ Resuelto
- **Root cause**: Schema mismatch - database usa INT pero JPA entities usaban Long
- Cambios aplicados a 14 domain models y 14 entities:
  - `translationId`: Long → Integer
  - `languageId`: Long → Integer
  - Removido `@ManyToOne` con `LanguageEntity`, reemplazado con `Integer languageId` (FK simple)
  - Actualizado queries en repositorios para usar `ct.languageId` en vez de `ct.language.languageId`
  - Removido `JpaAuditingEntity` inheritance de 8 translation entities (no tienen columnas de auditoría)
  - Fixed duplicate `publish()` methods
  - Actualizado mappers para usar `getLanguageId()` directamente

### Problema 3: Translation Processor Fallaba Silenciosamente
**Estado**: ✅ Resuelto completamente

**Bug #1 - Case Mismatch**: 
- Database almacena enums como UPPERCASE ('PENDING', 'COMPLETED')
- Repository pasaba `.toLowerCase()` en queries
- **Fix**: Removido `.toLowerCase()`, queries usan strings UPPERCASE

**Bug #2 - Type Mismatch**:
- JPA queries esperaban `TranslationJobStatus` enum pero recibían String
- Entity usa `@Enumerated(EnumType.STRING)` → Hibernate necesita enum type en queries
- **Fix**: 
  - Cambiado `TranslationJobJpaRepository` para aceptar `TranslationJobStatus` en vez de String
  - Added import de `TranslationJobStatus` en JPA repository
  - Repository impl ahora pasa enums directamente (no `.name()`)

**Bug #3 - Transaction Required**:
- `claimJob()` usa `@Modifying` UPDATE query pero se ejecutaba en thread async sin transacción
- **Fix**: 
  - Added `@Transactional` a `TranslationJobRepositoryImpl.claimJob()`
  - Added `@Transactional` a `TranslationProcessor.processJob()`

**Bug #4 - Logging**:
- `@Slf4j` estaba después de `@Component` (posición incorrecta)
- No había try-catch en método async
- **Fix**: 
  - Movido `@Slf4j` antes de `@Component`
  - Added try-catch wrapper en `processTranslationJobs()` con logging detallado

---

## Cambios en Base de Datos

### Changelog 092
Agregado `preferred_language_id BIGINT` a tabla `profile` para que usuarios puedan seleccionar idioma preferido.

### Schema de Translation Job
```sql
CREATE TABLE translation_job (
  job_id INT PRIMARY KEY AUTO_INCREMENT,
  entity_type VARCHAR(50) NOT NULL,
  entity_id INT NOT NULL,
  source_language_id INT NOT NULL,
  target_language_id INT NOT NULL,
  source_hash VARCHAR(64) NOT NULL,
  status VARCHAR(20) NOT NULL,  -- PENDING, PROCESSING, COMPLETED, FAILED
  provider VARCHAR(20) NOT NULL,
  attempts INT DEFAULT 0,
  last_error VARCHAR(1000),
  next_retry_at TIMESTAMP,
  completed_at TIMESTAMP,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  created_by VARCHAR(50),
  updated_by VARCHAR(50)
);
```

### Schema de Event Translation (ejemplo)
```sql
CREATE TABLE event_translation (
  translation_id INT PRIMARY KEY AUTO_INCREMENT,
  event_id BIGINT NOT NULL,
  language_id INT NOT NULL,
  translated_name VARCHAR(255),
  translated_description TEXT,
  source VARCHAR(20),  -- MACHINE, HUMAN, VERIFIED
  status VARCHAR(20),  -- DRAFT, PUBLISHED
  FOREIGN KEY (event_id) REFERENCES event(event_id),
  FOREIGN KEY (language_id) REFERENCES language(language_id),
  UNIQUE (event_id, language_id)
);
```

---

## Cambios en Backend

### Archivos Clave Modificados

**Domain & Application Layer:**
- `TranslationJob.java` - Domain model con métodos `markCompleted()`, `markFailed()`, `incrementAttempts()`
- `Language.java` - Domain model para idiomas (usa `@Getter` de Lombok)
- `TranslationService.java` - Servicio que crea jobs con `@Transactional(propagation = REQUIRES_NEW)`
- `TranslationProcessor.java` - Procesador async con `@Slf4j`, `@Transactional` en `processJob()`

**Infrastructure Layer:**
- `TranslationJobJpaRepository.java` - **CRÍTICO**: Todos los parámetros de status cambiados de String a `TranslationJobStatus`
- `TranslationJobRepositoryImpl.java` - **CRÍTICO**: Método `claimJob()` tiene `@Transactional`, pasa enums directamente
- `TranslationJobMapper.java` - Mapea entre entity y domain (status es enum en ambos)
- `EventTranslatableContentHandler.java` - Handler para traducir eventos
- `EventTranslationListener.java` - Listener con `@TransactionalEventListener(phase = AFTER_COMMIT)` que triggerea traducciones

**Configuration:**
- `TranslationConfig.java` - Define thread pool "translationExecutor" y registra handlers

### DTOs Actualizados

**EventResponse.java** - Agregados campos de traducción:
```java
private String translatedName;
private String translatedDescription;
private String languageCode;
```

**ProfileResponse.java & UpdateProfileRequest.java** - Agregado:
```java
private Integer preferredLanguageId;
```

### Controllers Actualizados

**EventController.java** - Endpoint `getEvent()` acepta parámetro opcional:
```java
@GetMapping("/{eventId}")
public ResponseEntity<ApiResponse<EventResponse>> getEvent(
    @PathVariable Long eventId,
    @RequestParam(required = false) String language
)
```

**ProfileController.java** - Maneja `preferredLanguageId` en updates

---

## Cambios en Frontend

### Nuevos Servicios

**LanguageService.js** - Nuevo servicio:
```javascript
export const getActiveLanguages = async () => {
  const response = await httpRequest.get('/api/v1/languages');
  return response.data;
};
```

**EventService.js** - Actualizado:
```javascript
export const getEventById = async (eventId, languageCode = null) => {
  const params = languageCode ? { language: languageCode } : {};
  const response = await httpRequest.get(`/api/v1/events/${eventId}`, { params });
  return response.data;
};
```

### Componentes Actualizados

**ProfilePage.jsx**:
- Carga idiomas activos de API
- Muestra selector de idioma preferido
- Guarda `preferredLanguageId` al actualizar perfil
- Carga idioma preferido del usuario al montar

**EventDetail.jsx**:
- Carga idiomas activos y idioma preferido del usuario
- Muestra selector de idioma en parte superior de info del evento
- Llama `getEventById(eventId, languageCode)` cuando cambia idioma
- Muestra `translatedName` y `translatedDescription` si están disponibles
- Badge indica idioma de traducción

### Hook Creado

**useTokenRefresh.js**:
- Refresca token JWT cada 12 minutos (en producción, 1 minuto en desarrollo)
- Usa `httpRequest` del proyecto (no axios)
- Llama `reconnectWebSocket(newToken)` después de refrescar
- Maneja errores y limpia interval al desmontar

---

## Estado Actual del Sistema

### ✅ Funcionando Correctamente

1. **Creación de eventos** → Genera 3 translation jobs (en, fr, pt) con status=PENDING
2. **TranslationProcessor** encuentra jobs pendientes
3. **Job claiming** funciona (UPDATE query exitoso con @Transactional)
4. **Carga de idiomas** (source y target) funciona
5. **Verificación de traducciones existentes** funciona
6. **Carga de textos fuente** funciona (event_name, description)
7. **Text masking service** funciona
8. **Frontend** carga idiomas, muestra selector, maneja cambios

### ⚠️ Bloqueado Actualmente

**LibreTranslate no responde** a requests de traducción:
- Container muestra status "healthy"
- Endpoint `/languages` funciona correctamente
- Endpoint `/translate` hace timeout (>10 segundos sin respuesta)
- Esto bloquea que el job complete

**Evidencia del problema**:
- Event 8 creado exitosamente
- Job 16 (event/8 → en) está en status=PROCESSING desde 23:36:54
- Logs muestran hasta "Masked text: 0 terms, 0 variables"
- Siguiente paso sería llamar LibreTranslate API, pero se cuelga ahí

**Último log exitoso**:
```
2026-10-12T23:36:54.807 DEBUG [translation-1] c.c.t.m.l.a.service.TextMaskingService : Masked text: 0 terms, 0 variables
```

---

## Próximos Pasos Sugeridos

### Opción A: Fix LibreTranslate
1. Revisar logs completos del container Docker
2. Verificar recursos (CPU, memoria)
3. Restart container con límites de recursos ajustados
4. Verificar que modelos de traducción estén cargados correctamente
5. Test manual con `curl` hasta que responda

### Opción B: Provider Alternativo
1. Configurar Google Translate API (requiere API key)
2. Crear `GoogleTranslateAdapter` implementando `TranslationPort`
3. Cambiar configuración en `application.properties`
4. Retest con nuevo provider

### Opción C: Mock para Testing
1. Crear `MockTranslationAdapter` que retorne texto simple
2. Verificar que todo el flujo funciona end-to-end
3. Luego volver a provider real

---

## Testing del Sistema

### Test Manual Completo

1. **Verificar idiomas disponibles**:
   ```bash
   curl http://localhost:8080/api/v1/languages
   # Debe retornar: [{"languageId":1,"code":"es",...}, ...]
   ```

2. **Crear evento** (genera jobs automáticamente)

3. **Verificar jobs creados**:
   ```sql
   SELECT job_id, entity_id, target_language_id, status 
   FROM translation_job 
   WHERE entity_type = 'event' AND entity_id = 8;
   ```

4. **Verificar que se procesen** (cuando LibreTranslate funcione):
   - Logs deben mostrar: "Successfully completed translation job X for event/8 -> en"
   - Status debe cambiar a COMPLETED
   
5. **Verificar traducciones guardadas**:
   ```sql
   SELECT * FROM event_translation WHERE event_id = 8;
   -- Debe tener 3 filas con translated_name y translated_description
   ```

6. **Test desde frontend**:
   - Ir a detalle de evento
   - Seleccionar idioma diferente
   - Verificar que nombre y descripción cambien
   - Badge debe mostrar idioma de traducción

---

## Configuración Importante

### Backend (application.properties)
```properties
# Translation config
translation.enabled=true
translation.provider=libretranslate
translation.libretranslate.url=http://localhost:5001
translation.backoff.max-minutes=60
translation.executor.core-pool-size=2
translation.executor.max-pool-size=4
translation.executor.queue-capacity=100
```

### Docker Compose (LibreTranslate)
```yaml
libretranslate:
  image: libretranslate/libretranslate:v1.9.6
  ports:
    - "127.0.0.1:5001:5000"
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:5000/languages"]
```

---

## Archivos Importantes para Referencia

### Backend Core
- `TuEventoBackend/tu-evento/src/main/java/com/capysoft/tuevento/modules/language/`
  - `application/service/TranslationProcessor.java` ⭐
  - `application/service/TranslationService.java` ⭐
  - `application/listener/EventTranslationListener.java` ⭐
  - `domain/model/TranslationJob.java`
  - `domain/repository/TranslationJobRepository.java`
  - `infrastructure/persistence/repository/TranslationJobJpaRepository.java` ⭐
  - `infrastructure/persistence/repository/TranslationJobRepositoryImpl.java` ⭐
  - `infrastructure/translator/EventTranslatableContentHandler.java`
  - `infrastructure/config/TranslationConfig.java`

### Frontend Core
- `TuEventoFrontend/web/src/`
  - `services/LanguageService.js` (nuevo)
  - `services/EventService.js` (actualizado)
  - `pages/EventDetail.jsx` (actualizado)
  - `pages/ProfilePage.jsx` (actualizado)
  - `hooks/useTokenRefresh.js` (nuevo)

### Database
- `TuEventoBackend/tu-evento/src/main/resources/db/changelog/changes/`
  - `089-create-additional-translation-tables.yaml`
  - `090-create-more-translation-tables.yaml`
  - `091-create-final-translation-tables.yaml`
  - `092-add-preferred-language-to-profile.yaml`

---

## Notas de Implementación

### Convenciones de Código
- Commits en inglés con conventional commits
- Logs en español para debugging
- DTOs usan camelCase
- Database usa snake_case

### Tipos de Datos Críticos
- **translation_id**: INT (no BIGINT)
- **language_id**: INT (no BIGINT)
- **event_id**: BIGINT
- **job_id**: INT
- **status**: ENUM como STRING en database (PENDING, PROCESSING, COMPLETED, FAILED)

### Transacciones
- `EventTranslationListener` usa `@TransactionalEventListener(AFTER_COMMIT)` para ejecutar después del commit
- `TranslationService.requestTranslation()` usa `@Transactional(propagation = REQUIRES_NEW)` para nueva transacción
- `TranslationJobRepositoryImpl.claimJob()` requiere `@Transactional` por ser UPDATE query
- `TranslationProcessor.processJob()` requiere `@Transactional` para operaciones de DB

---

## Debugging Tips

### Ver logs del processor
```bash
# Backend debe mostrar:
DEBUG [translation-1] c.c.t.m.l.a.s.TranslationProcessor : Processing translation jobs for event/X
INFO  [translation-1] c.c.t.m.l.a.s.TranslationProcessor : Found 3 pending jobs for event/X
INFO  [translation-1] c.c.t.m.l.a.s.TranslationProcessor : Successfully completed translation job X
```

### Verificar LibreTranslate
```bash
# Check health
docker ps | grep libretranslate

# Test languages endpoint
curl http://localhost:5001/languages

# Test translate endpoint
curl -X POST http://localhost:5001/translate \
  -H "Content-Type: application/json" \
  -d '{"q":"Hola mundo","source":"es","target":"en","format":"text"}'
```

### Queries útiles
```sql
-- Ver todos los jobs
SELECT * FROM translation_job ORDER BY created_at DESC;

-- Ver jobs de un evento específico
SELECT * FROM translation_job WHERE entity_type = 'event' AND entity_id = 8;

-- Ver traducciones guardadas
SELECT * FROM event_translation WHERE event_id = 8;

-- Ver idiomas activos
SELECT * FROM language WHERE is_active = true;
```

---

**Fecha última actualización**: 2026-10-12
**Estado general**: Backend completamente funcional, esperando fix de LibreTranslate
**Próxima sesión**: Resolver timeout de LibreTranslate y verificar flujo completo end-to-end
