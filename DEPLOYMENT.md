# Documento de Despliegue — TuEvento
## Plataforma de destino: Render.com

---

# 1. Información General y Control de Versiones

## 1.1 Ficha Técnica del Despliegue

### 1.1.1 Identificación del sistema y versión

| Campo | Valor |
|---|---|
| **Nombre del sistema** | TuEvento |
| **Organización** | CapySoft |
| **Artifact ID** | `com.capysoft / tu-evento` |
| **Release Tag** | `v1.0.0-beta` (Fase 1 — Backend) |
| **Repositorio** | Monorepo `/TuEvento` |
| **Rama de despliegue** | `main` / `master` |

### 1.1.2 Fecha, hora y ventana operativa programada

| Campo | Valor |
|---|---|
| **Fecha planeada** | Por definir |
| **Hora de inicio** | Por definir |
| **Duración estimada** | 30–45 minutos (Fase 1) |
| **Ventana de rollback** | 15 minutos post-despliegue |
| **Zona horaria** | UTC-5 (Colombia) |

### 1.1.3 Entorno de destino

| Campo | Valor |
|---|---|
| **Plataforma** | Render.com (PaaS) |
| **Fase actual** | Fase 1 — Backend API + Base de Datos |
| **Región** | Oregon (us-west-2) |
| **Tipo de entorno** | Staging / Pruebas |
| **URL base backend** | `https://tuevento-backend.onrender.com` |
| **URL base frontend** | Pendiente — Fase 2 |

### 1.1.4 Matriz de roles y responsables

| Rol | Responsabilidad | Acción en este despliegue |
|---|---|---|
| **Tech Lead / DevOps** | Configuración de render.yaml, Dockerfile, variables de entorno | Ejecuta el Runbook (Sección 4) |
| **DBA** | Validación de migraciones Liquibase, integridad de esquemas | Verifica Sección 5.1.5 y 5.2.2 |
| **QA** | Ejecución de Smoke Tests funcionales | Ejecuta Sección 5.2 |
| **Backend Dev** | Revisión de logs de inicio, health checks | Monitorea Sección 5.3 |

---

## 1.2 Historial de Versiones del Documento

| Versión | Fecha | Autor | Descripción |
|---|---|---|---|
| 1.0.0 | 2026-10-22 | CapySoft | Creación inicial — Fase 1 Backend en Render |

---

## 1.3 Resumen de Cambios (Release Notes)

### 1.3.1 Nuevas funcionalidades incluidas en este release

Las siguientes funcionalidades están implementadas y se despliegan en esta versión:

**Módulo de Seguridad**
- Registro con validación Gmail, contraseña fuerte, formato de nombre completo
- Activación de cuenta por código de verificación
- Login con JWT (access token 15 min, refresh token 7 días)
- OAuth2 con Google y Facebook
- Recuperación y cambio de contraseña
- Bloqueo de cuenta por intentos fallidos
- Gestión de solicitudes de organizador (admin)

**Módulo de Perfil**
- Creación y actualización de perfil de usuario
- Foto de perfil con avatar por defecto en S3
- Log de cambios de perfil

**Módulo de Eventos**
- CRUD completo de eventos (organizador)
- Máquina de estados: `DRAFT → PENDING_REVIEW → PUBLISHED → COMPLETED / CANCELLED / REJECTED`
- Layout visual del evento (editor de secciones y sillas con Konva)
- Subida de media con moderación de contenido (OpenNSFW2 + Sightengine)
- Comentarios con estrellas, respuestas, menciones estilo TikTok, edición 2h
- Notificaciones in-app y email al publicar/rechazar eventos

**Módulo de Sillas y Secciones**
- Generación automática de sillas desde el layout
- Reserva temporal con TTL de 10 minutos
- Expiración automática (scheduler cada 10s)
- WebSocket en tiempo real para actualizaciones de estado
- Sub-secciones con numeración continua

**Módulo de Tickets y Órdenes**
- Creación de órdenes con tickets
- Máquina de estados de orden: `DRAFT → PAYMENT_PENDING → PAID → USED / REFUNDED`
- Check-in con código QR
- Historial de tickets por usuario

**Módulo de Pagos**
- Integración con Fake Payment Gateway (entorno de desarrollo)
- Procesamiento de webhooks con firma HMAC-SHA256
- Cartera (wallet) con créditos, pagos parciales y reversiones
- Reembolsos

**Módulos de soporte**
- Categorías jerárquicas de eventos
- Geolocalización (33 departamentos, 1.122 ciudades de Colombia)
- Temas visuales (4 paletas base: Dark, Light, Vibrant, Accessible)
- Módulo de idiomas
- Notificaciones (in-app + email)
- Almacenamiento S3/MinIO con moderación de contenido

### 1.3.2 Correcciones de errores incluidas

- Fix de vulnerabilidad: código de activación no validado contra email del request
- Fix de vulnerabilidad: código de recuperación no validado contra email del request
- Fix de vulnerabilidad: logout sin validación de ownership de sesión
- Fix: login no verificaba estados BLOCKED/INACTIVE/DELETED
- Fix: OAuth permitía email duplicado con cuenta local
- Fix: renderizado de 999+ sillas optimizado (React.memo + índice centralizado)
- Fix: race condition WebSocket/POST en comentarios (idempotencia por ratingId)
- Fix: sistema de paginación bidireccional para secciones de forma irregular

### 1.3.3 Cambios de infraestructura y dependencias

| Componente | Antes (local) | Después (Render/externo) |
|---|---|---|
| Base de datos | Docker PostgreSQL local | Render PostgreSQL (managed) |
| Cache/Sesiones | Redis Docker local | Upstash Redis (free tier, TLS) |
| Almacenamiento | MinIO Docker local | AWS S3 / MinIO existente |
| TLS/HTTPS | Manual / ninguno | Render (automático, Let's Encrypt) |
| Migraciones | Liquibase en startup | Liquibase en startup (sin cambios) |

### 1.3.4 Cambios incompatibles (Breaking changes)

| # | Descripción | Acción requerida |
|---|---|---|
| 1 | `SPRING_DATASOURCE_URL` de Render usa formato `postgres://` — Spring necesita `jdbc:postgresql://` | El `render.yaml` ya inyecta la URL correcta via `fromDatabase.connectionString` — **verificar en logs de inicio** |
| 2 | Redis en Upstash requiere TLS (`ssl.enabled: true`) | `application-prod.yml` ya lo configura — **no usar Redis sin TLS** |
| 3 | Perfil activo cambia de `dev` a `prod` | Variable `SPRING_PROFILES_ACTIVE=prod` configurada en `render.yaml` |
| 4 | CORS restringido a dominio del frontend | Actualizar `APP_CORS_ALLOWED_ORIGINS` cuando se despliegue el frontend en Fase 2 |

---

# 2. Alcance, Topología y Arquitectura

## 2.1 Componentes y Artefactos en Alcance

### 2.1.1 Panel de administración

Render no usa Portainer. El panel de administración es el **Dashboard de Render** en `https://dashboard.render.com`. Desde ahí se gestionan servicios, logs, variables de entorno y deploys.

### 2.1.2 Capa Frontend Web (Fase 2 — pendiente)

| Campo | Valor |
|---|---|
| **Framework** | React 18 + Vite 5 |
| **Estilos** | Tailwind CSS v4 |
| **Librerías clave** | react-konva, leaflet, @stomp/stompjs, sockjs-client, lucide-react |
| **Servidor en producción** | Nginx (Static Site en Render) |
| **Build command** | `npm run build` |
| **Directorio de salida** | `dist/` |

### 2.1.3 Capa Backend API

| Campo | Valor |
|---|---|
| **Framework** | Spring Boot 3.5.x |
| **Java** | 17 (compilación) / 21 JRE (runtime) |
| **Puerto** | 8080 |
| **Migraciones** | Liquibase (integrado, corre al iniciar) |
| **Health check** | `GET /actuator/health` |
| **API docs** | `GET /swagger-ui` |
| **Autenticación** | JWT (JJWT 0.12.6) + OAuth2 (Google, Facebook) |
| **WebSocket** | STOMP sobre SockJS (`/ws`) |

Dependencias principales:

| Dependencia | Versión | Función |
|---|---|---|
| spring-boot | 3.5.12 | Framework base |
| spring-security | (managed) | Autenticación y autorización |
| spring-data-jpa | (managed) | ORM con Hibernate |
| spring-data-redis | (managed) | Cache y sesiones |
| spring-kafka | (managed) | Mensajería asíncrona |
| liquibase-core | (managed) | Migraciones de BD |
| springdoc-openapi | 2.8.8 | Swagger UI |
| jjwt | 0.12.6 | Generación y validación de JWT |
| mapstruct | 1.6.3 | Mapeo de objetos |
| aws-sdk s3 | 2.25.60 | Cliente S3/MinIO |
| lombok | (managed) | Reducción de boilerplate |

### 2.1.4 Capa de Persistencia

| Campo | Valor |
|---|---|
| **Motor** | PostgreSQL (managed por Render) |
| **Plan** | Free (90 días, 1 GB storage, 25 conexiones máx) |
| **Pool de conexiones** | HikariCP — máx 5 conexiones (configurado para free tier) |
| **Esquema** | Gestionado exclusivamente por Liquibase |
| **Changesets** | 090 migraciones acumuladas |

---

## 2.2 Diagrama de Arquitectura en Render

```
┌─────────────────────────────────────────────────────────────────┐
│                        INTERNET / USUARIOS                       │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTPS (TLS automático — Render)
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    RENDER LOAD BALANCER                          │
│              (reverse proxy + TLS termination)                   │
└──────────────┬──────────────────────────────┬───────────────────┘
               │                              │
               ▼                              ▼
┌──────────────────────────┐    ┌─────────────────────────────┐
│   tuevento-frontend      │    │    tuevento-backend          │
│   Static Site (Nginx)    │    │    Web Service (Docker)      │
│   React 18 + Vite        │◄──►│    Spring Boot 3.5 / JRE 21  │
│   :443 → dist/           │    │    :8080                     │
│   [FASE 2]               │    │    /actuator/health          │
└──────────────────────────┘    │    /swagger-ui               │
                                │    /ws (WebSocket STOMP)     │
                                └──────────┬──────────────────┘
                                           │
               ┌───────────────────────────┼───────────────────┐
               │                           │                   │
               ▼                           ▼                   ▼
┌──────────────────────┐  ┌───────────────────────┐  ┌────────────────────┐
│   tuevento-db        │  │   Upstash Redis        │  │   AWS S3 / MinIO   │
│   PostgreSQL         │  │   (externo, TLS)       │  │   (externo)        │
│   Render Managed     │  │   Cache + Sesiones     │  │   Archivos + Media │
│   :5432 (interno)    │  │   :6379                │  │   bucket: tuevento │
└──────────────────────┘  └───────────────────────┘  └────────────────────┘
```

### 2.2.1 Topología de red en Render

- Todos los servicios del mismo proyecto en Render se comunican por **red interna privada**
- PostgreSQL solo acepta conexiones desde servicios internos de Render (`ipAllowList: []`)
- Redis (Upstash) es externo — se conecta por TLS sobre internet público con autenticación por contraseña
- S3/MinIO es externo — se conecta por HTTPS desde el backend

### 2.2.2 TLS / HTTPS

Render gestiona automáticamente los certificados TLS via Let's Encrypt para todos los servicios. No se requiere configuración manual. El backend tiene `forward-headers-strategy: framework` para leer correctamente el esquema `https://` en los redirect URIs de OAuth.

---

## 2.3 Matriz de Ambientes, Puertos y URLs

| Servicio | Entorno local | URL en Render (Fase 1) |
|---|---|---|
| Backend API | `http://localhost:8080` | `https://tuevento-backend.onrender.com` |
| PostgreSQL | `localhost:5432` | Interno Render (no expuesto) |
| Redis | `localhost:6379` | Upstash (externo, TLS) |
| S3/MinIO | `http://localhost:9000` | AWS S3 / MinIO externo |
| Frontend Web | `http://localhost:5173` | Pendiente — Fase 2 |
| Fake Gateway | `http://localhost:4001` | Pendiente — Fase 3 |
| nsfw-classifier | `http://localhost:8001` | Pendiente — Fase 3 |
| Swagger UI | `http://localhost:8080/swagger-ui` | `https://tuevento-backend.onrender.com/swagger-ui` |
| Health Check | `http://localhost:8080/actuator/health` | `https://tuevento-backend.onrender.com/actuator/health` |

---

# 3. Requisitos Previos y Preparación

## 3.1 Checklist de Prerrequisitos

### 3.1.1 Cuentas y accesos necesarios (TÚ verificas)

- [ ] Cuenta activa en [render.com](https://render.com) con tarjeta registrada (requerida aunque uses free tier)
- [ ] Repositorio Git accesible por Render (GitHub, GitLab o Bitbucket)
- [ ] Cuenta en [upstash.com](https://upstash.com) para Redis gratuito
- [ ] Acceso a las credenciales de AWS S3 / MinIO existente
- [ ] App Password de Gmail configurada (no la contraseña de la cuenta)
- [ ] Credenciales de Google OAuth (Google Cloud Console)
- [ ] Credenciales de Facebook OAuth (Meta Developers)
- [ ] Cuenta en [sightengine.com](https://sightengine.com) (free: 100 req/día)

### 3.1.2 Estado del repositorio (TÚ verificas)

- [ ] La rama de despliegue (`main`) tiene los 3 archivos nuevos commiteados:
  - `render.yaml`
  - `TuEventoBackend/tu-evento/Dockerfile` (actualizado)
  - `TuEventoBackend/tu-evento/src/main/resources/application-prod.yml`
- [ ] El archivo `.env` y `.env.example` NO están en la rama (están en `.gitignore`)

---

## 3.2 Estrategia de Respaldos

> En Fase 1 (primer despliegue desde cero) no hay datos previos que respaldar.
> Este procedimiento aplica a partir del segundo despliegue.

### 3.2.1 Respaldo de base de datos

Render PostgreSQL (free tier) no incluye backups automáticos. Antes de cada deploy:

```bash
# Desde tu máquina local, con acceso a la URL de Render
pg_dump \
  --no-owner \
  --no-acl \
  -F c \
  -f backup_tuevento_$(date +%Y%m%d_%H%M%S).dump \
  "postgresql://USUARIO:PASSWORD@HOST:5432/tuevento"
```

Guardar el archivo `.dump` en un lugar seguro (Google Drive, S3, etc.).

### 3.2.2 Respaldo de variables de entorno

Antes de modificar variables en el Dashboard de Render, exportarlas como referencia:

1. Dashboard de Render → tuevento-backend → **Environment**
2. Copiar todos los valores en un archivo local cifrado (NO subirlo al repo)

### 3.2.3 Rollback de código

Render guarda el historial de deploys:

1. Dashboard → tuevento-backend → **Deploys**
2. Hacer clic en el deploy anterior → **Rollback to this deploy**

---

## 3.3 Gestión de Variables de Entorno y Secretos

Estas son **todas** las variables que necesitas configurar en Render. Las marcadas con `sync: false` en el `render.yaml` te las pedirá Render al momento de conectar el repositorio.

### 3.3.1 Variables de base de datos

Render las inyecta automáticamente desde `tuevento-db` — no las configures manualmente:

| Variable | Fuente |
|---|---|
| `SPRING_DATASOURCE_URL` | Auto — `fromDatabase.connectionString` |
| `POSTGRES_USER` | Auto — `fromDatabase.user` |
| `POSTGRES_PASSWORD` | Auto — `fromDatabase.password` |
| `POSTGRES_DB` | Auto — `fromDatabase.database` |

### 3.3.2 Variables que TÚ configuras manualmente en Render

Después de crear el servicio, ir a **tuevento-backend → Environment → Add Environment Variable**:

**Redis (Upstash):**

| Variable | Dónde obtenerla | Ejemplo |
|---|---|---|
| `SPRING_REDIS_HOST` | Upstash Console → tu instancia → Endpoint | `apn1-XXX.upstash.io` |
| `SPRING_REDIS_PASSWORD` | Upstash Console → tu instancia → Password | `AXXXxxxxxxx` |

**JWT:**

| Variable | Descripción |
|---|---|
| `JWT_SECRET` | Cadena aleatoria de mínimo 32 caracteres. Generar con: `openssl rand -hex 32` |

**Correo:**

| Variable | Descripción |
|---|---|
| `MAIL_USERNAME` | Tu cuenta Gmail: `tucuenta@gmail.com` |
| `MAIL_PASSWORD` | App Password de Google (16 caracteres sin espacios) |

**OAuth Google** (desde [console.cloud.google.com](https://console.cloud.google.com)):

| Variable | Descripción |
|---|---|
| `GOOGLE_CLIENT_ID` | ID del cliente OAuth 2.0 |
| `GOOGLE_CLIENT_SECRET` | Secret del cliente OAuth 2.0 |
| `APP_OAUTH_GOOGLE_REDIRECT_URI` | `https://tuevento-backend.onrender.com` |

**OAuth Facebook** (desde [developers.facebook.com](https://developers.facebook.com)):

| Variable | Descripción |
|---|---|
| `FACEBOOK_CLIENT_ID` | App ID |
| `FACEBOOK_CLIENT_SECRET` | App Secret |
| `APP_OAUTH_FACEBOOK_REDIRECT_URI` | `https://tuevento-backend.onrender.com` |

**AWS S3 / MinIO:**

| Variable | Descripción |
|---|---|
| `APP_STORAGE_ENDPOINT` | URL del endpoint S3: `https://s3.amazonaws.com` |
| `MINIO_PUBLIC_URL` | URL pública para presigned URLs del navegador |
| `MINIO_ROOT_USER` | AWS Access Key ID |
| `MINIO_ROOT_PASSWORD` | AWS Secret Access Key |

**Admin inicial:**

| Variable | Descripción |
|---|---|
| `ADMIN_EMAIL` | Email del administrador del sistema |
| `ADMIN_PASSWORD` | Contraseña fuerte (mínimo 8 chars, mayúscula, número, símbolo) |

**Pagos y webhooks:**

| Variable | Descripción |
|---|---|
| `WEBHOOK_SECRET` | Cadena aleatoria: `openssl rand -hex 32` |
| `FAKE_GATEWAY_URL` | `https://tuevento-fake-gateway.onrender.com` (Fase 3) |

**Moderación (Sightengine):**

| Variable | Descripción |
|---|---|
| `SIGHTENGINE_API_USER` | User de [sightengine.com](https://sightengine.com) |
| `SIGHTENGINE_API_SECRET` | Secret de Sightengine |

**CORS:**

| Variable | Descripción |
|---|---|
| `APP_CORS_ALLOWED_ORIGINS` | `https://tuevento-frontend.onrender.com` (actualizar en Fase 2) |

### 3.3.3 Límites de memoria JVM

El `Dockerfile` ya configura los flags correctos para el free tier de Render (512 MB RAM):

```
-XX:+UseContainerSupport      # Lee límites reales del contenedor
-XX:MaxRAMPercentage=75.0     # Heap = 75% de 512 MB ≈ 384 MB
```

No se necesita configuración adicional.

---

## 3.4 Protocolo de Comunicación y Ventana de Mantenimiento

- **Canal de coordinación**: WhatsApp / Discord del equipo
- **Notificar antes de iniciar**: avisar al equipo con 15 minutos de anticipación
- **Tiempo estimado**: 30–45 minutos para el primer despliegue (el build Maven tarda ~10 min)
- **Criterio de éxito**: `/actuator/health` devuelve `{"status":"UP"}` con todos los componentes
- **Criterio de rollback**: si después de 15 minutos el health check no responde o Liquibase falla

---

# 4. Procedimiento de Despliegue Paso a Paso (Runbook)

> **Quién ejecuta esto:** El Tech Lead / DevOps del equipo.
> Render se encarga del build Docker, el TLS y la base de datos. Tu trabajo es conectar el repositorio y configurar las variables.

---

## 4.1 Paso 1 — Commit y push de los archivos nuevos

**TÚ ejecutas en tu máquina:**

```bash
# Desde la raíz del proyecto
git add render.yaml
git add TuEventoBackend/tu-evento/Dockerfile
git add TuEventoBackend/tu-evento/src/main/resources/application-prod.yml

git commit -m "chore(deploy): add Render config files for Phase 1 deployment"
git push origin main
```

Verificar que `.env` y `.env.example` **NO** están en el commit (deben estar en `.gitignore`).

---

## 4.2 Paso 2 — Crear instancia de Redis en Upstash

**TÚ haces esto en [upstash.com](https://upstash.com):**

1. Registrarse / iniciar sesión en Upstash
2. **Create Database** → tipo **Redis**
3. Nombre: `tuevento-redis`
4. Región: `US-West-1` (más cercana a Oregon donde está Render)
5. Plan: **Free** (10.000 comandos/día)
6. Hacer clic en **Create**
7. En el detalle de la instancia, copiar:
   - **Endpoint** → guardar como `SPRING_REDIS_HOST`
   - **Password** → guardar como `SPRING_REDIS_PASSWORD`
   - Asegurarse de que **TLS** está activado

---

## 4.3 Paso 3 — Conectar el repositorio en Render

**TÚ haces esto en [dashboard.render.com](https://dashboard.render.com):**

1. Hacer clic en **New +** → **Blueprint**
2. Conectar tu cuenta de GitHub/GitLab si no lo has hecho
3. Seleccionar el repositorio `TuEvento`
4. Render detectará el archivo `render.yaml` automáticamente
5. Render mostrará un resumen de lo que va a crear:
   - `tuevento-db` (PostgreSQL)
   - `tuevento-backend` (Web Service Docker)
6. Render pedirá los valores de las variables marcadas con `sync: false`
7. Completar **todas** las variables de la Sección 3.3.2 antes de continuar
8. Hacer clic en **Apply** para iniciar el despliegue

---

## 4.4 Paso 4 — Despliegue y migración de base de datos

**Render hace esto automáticamente:**

1. Crea la base de datos `tuevento-db` en PostgreSQL
2. Construye la imagen Docker del backend (Maven build ~10 minutos)
3. Inicia el contenedor con las variables de entorno inyectadas
4. Spring Boot arranca con `SPRING_PROFILES_ACTIVE=prod`
5. **Liquibase ejecuta automáticamente** los 90 changesets en orden

**TÚ monitoreas:**

- Dashboard → tuevento-backend → **Logs** (en tiempo real)
- Buscar la línea: `Liquibase: Update has been successful.`
- Buscar la línea: `Started TuEventoApplication in X.XXX seconds`

Si Liquibase falla, el log indicará exactamente qué changeset falló y por qué.

### 4.4.1 Verificación de integridad del esquema

Una vez que el servicio está `Live`, ejecutar desde tu máquina:

```bash
# Reemplazar con los valores reales de Render
psql "postgresql://USUARIO:PASSWORD@HOST:5432/tuevento" \
  -c "SELECT COUNT(*) as total_tablas FROM information_schema.tables WHERE table_schema = 'public';"

# Resultado esperado: ~45 tablas (una por cada entidad del sistema)

# Verificar que Liquibase terminó correctamente
psql "postgresql://USUARIO:PASSWORD@HOST:5432/tuevento" \
  -c "SELECT id, filename, exectype FROM databasechangelog ORDER BY dateexecuted DESC LIMIT 5;"
```

---

## 4.5 Paso 5 — Configuración de CORS y OAuth (post-deploy)

Una vez que Render asigna la URL definitiva al servicio:

1. Anotar la URL: `https://tuevento-backend.onrender.com` (o la URL asignada)

2. **Google Cloud Console** → Credenciales → tu cliente OAuth:
   - Agregar a "URI de redireccionamiento autorizados":
     `https://tuevento-backend.onrender.com/api/v1/auth/oauth/google/callback`

3. **Meta Developers** → tu app → Configuración de OAuth:
   - Agregar URI de redireccionamiento:
     `https://tuevento-backend.onrender.com/api/v1/auth/oauth/facebook/callback`

4. En Render → tuevento-backend → **Environment**, actualizar:
   - `APP_OAUTH_GOOGLE_REDIRECT_URI` = `https://tuevento-backend.onrender.com`
   - `APP_OAUTH_FACEBOOK_REDIRECT_URI` = `https://tuevento-backend.onrender.com`
   - Guardar → Render redespliega automáticamente

---

# 5. Plan de Verificación, Pruebas y Evidencias (Smoke Tests)

> **Quién ejecuta esto:** QA y Backend Dev, después de que el servicio muestre estado `Live` en Render.

---

## 5.1 Matriz de Endpoints de Verificación

| # | Servicio | URL | Método | Resultado esperado |
|---|---|---|---|---|
| 1 | Panel Render | `https://dashboard.render.com` | Browser | Servicio `tuevento-backend` en estado **Live** |
| 2 | Health Check | `https://tuevento-backend.onrender.com/actuator/health` | GET | HTTP 200, `{"status":"UP"}` |
| 3 | Swagger UI | `https://tuevento-backend.onrender.com/swagger-ui` | Browser | Interfaz Swagger con todos los módulos |
| 4 | API Docs JSON | `https://tuevento-backend.onrender.com/api-docs` | GET | HTTP 200, JSON OpenAPI válido |
| 5 | BD (interno) | Logs de Liquibase | Logs Render | `Update has been successful` |

### 5.1.1 Captura esperada — Render Dashboard

El panel debe mostrar:
- `tuevento-db`: estado `Available`
- `tuevento-backend`: estado `Live` (punto verde)

### 5.1.2 Interfaz web pública (Fase 2 — pendiente)

No aplica en Fase 1. El frontend se despliega en Fase 2.

### 5.1.3 Health Check del Backend

```bash
curl -s https://tuevento-backend.onrender.com/actuator/health | python3 -m json.tool
```

Respuesta esperada:
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" },
    "mail": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

> **Nota:** Si `redis` aparece `DOWN`, verificar las variables `SPRING_REDIS_HOST`, `SPRING_REDIS_PASSWORD` y `SPRING_REDIS_SSL=true` en el Dashboard de Render.

### 5.1.4 Swagger UI

Navegar a `https://tuevento-backend.onrender.com/swagger-ui`

Verificar que aparecen los siguientes controladores:
- `auth-controller`
- `event-controller`
- `ticket-controller`
- `payment-controller`
- `wallet-controller`
- `category-controller`
- `profile-controller`
- `theme-controller`
- `geolocation-controller`

### 5.1.5 Conectividad a base de datos

Verificar desde los logs de inicio de Render que Liquibase ejecutó sin errores:

```
Buscar en los logs: "Successfully acquired change log lock"
Buscar en los logs: "Update has been successful"
Buscar en los logs: "Successfully released change log lock"
```

---

## 5.2 Pruebas de Humo Funcionales (Smoke Tests)

Usar Swagger UI o Postman/Insomnia.

### 5.2.1 Flujo de autenticación y emisión de tokens

**Test 1 — Registro de usuario:**

```http
POST https://tuevento-backend.onrender.com/api/v1/auth/register
Content-Type: application/json

{
  "email": "test.smoke@gmail.com",
  "password": "TestSmoke123@",
  "fullName": "Test Smoke User"
}
```

Resultado esperado: HTTP 201, mensaje de activación enviado al correo.

**Test 2 — Activación de cuenta:**

Revisar el correo `test.smoke@gmail.com`, copiar el código de 6 dígitos:

```http
POST https://tuevento-backend.onrender.com/api/v1/auth/activate
Content-Type: application/json

{
  "email": "test.smoke@gmail.com",
  "code": "XXXXXX"
}
```

Resultado esperado: HTTP 200.

**Test 3 — Login y obtención de token:**

```http
POST https://tuevento-backend.onrender.com/api/v1/auth/login
Content-Type: application/json

{
  "email": "test.smoke@gmail.com",
  "password": "TestSmoke123@"
}
```

Resultado esperado: HTTP 200 con `accessToken` y `refreshToken` en la respuesta.

Guardar el `accessToken` para los tests siguientes.

### 5.2.2 Pruebas de lectura y consulta de datos

**Test 4 — Listar categorías (público):**

```http
GET https://tuevento-backend.onrender.com/api/v1/categories
```

Resultado esperado: HTTP 200, array con las 5 categorías seed del sistema.

**Test 5 — Listar departamentos (público):**

```http
GET https://tuevento-backend.onrender.com/api/v1/departments
```

Resultado esperado: HTTP 200, array con los 33 departamentos de Colombia.

**Test 6 — Perfil del usuario autenticado:**

```http
GET https://tuevento-backend.onrender.com/api/v1/profiles/user/{userId}
Authorization: Bearer {accessToken}
```

Resultado esperado: HTTP 200 con datos del perfil.

### 5.2.3 Pruebas de escritura y persistencia transaccional

**Test 7 — Crear perfil:**

```http
POST https://tuevento-backend.onrender.com/api/v1/profiles
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "userId": {userId_del_test_1},
  "fullName": "Test Smoke User"
}
```

Resultado esperado: HTTP 201 con el perfil creado y avatar por defecto asignado.

**Test 8 — Activar tema:**

```http
POST https://tuevento-backend.onrender.com/api/v1/themes/activate/1
Authorization: Bearer {accessToken}
```

Resultado esperado: HTTP 200 con la paleta de colores del tema DARK.

---

## 5.3 Inspección de Logs y Monitoreo de Recursos

### 5.3.1 Revisión de trazas de inicio

En Render → tuevento-backend → **Logs**, verificar la secuencia de arranque esperada:

```
[INFO] Starting TuEventoApplication
[INFO] Successfully acquired change log lock
[INFO] Running Changeset: ...001-create-...
...
[INFO] Update has been successful.
[INFO] Successfully released change log lock
[INFO] Started TuEventoApplication in XX.XXX seconds (JVM running for XX.XXX)
```

**Líneas de error que indican problema** (si aparecen, el deploy falló):
- `Unable to acquire Changelog Lock` → Liquibase bloqueado (solución: borrar fila en `databasechangeloglock`)
- `Connection refused` a Redis → verificar variables de Upstash
- `org.postgresql.util.PSQLException` → verificar URL de base de datos
- `UnsatisfiedDependencyException` → variable de entorno requerida no configurada

### 5.3.2 Monitoreo de CPU y memoria

En Render → tuevento-backend → **Metrics**:

| Métrica | Valor normal en free tier |
|---|---|
| RAM usada | 250–400 MB (de 512 MB disponibles) |
| CPU | < 20% en idle |
| Heap JVM | ~200–300 MB (75% de RAM) |

> **Nota free tier:** El servicio se duerme después de 15 minutos sin tráfico. El primer request tarda ~30 segundos en despertar. Para staging esto es aceptable; para producción se recomienda el plan `starter` ($7/mes).

---

## 5.4 Registro de Evidencias Visuales

> **Quién las toma:** El Tech Lead o DevOps al finalizar el despliegue.
> Guardar las capturas en la carpeta del proyecto o en el canal del equipo.

### 5.4.1 Captura — Render Dashboard con servicios activos

**Qué capturar:** Pantalla del Dashboard mostrando:
- `tuevento-db` con estado **Available** (punto verde)
- `tuevento-backend` con estado **Live** (punto verde)
- Timestamp del último deploy exitoso

**Cómo:** Dashboard de Render → sección **Services** del proyecto

### 5.4.2 Captura — Logs de inicio del Backend

**Qué capturar:** Ventana de logs de Render mostrando:
- Las últimas líneas del startup de Spring Boot
- La línea `Started TuEventoApplication in X.XXX seconds`
- Sin líneas de ERROR ni WARN críticos

**Cómo:** Dashboard → tuevento-backend → pestaña **Logs**

### 5.4.3 Captura — Respuesta HTTP 200 del Health Check

**Qué capturar:** Respuesta del endpoint `/actuator/health` mostrando:
- Status HTTP `200 OK`
- Body JSON con `"status": "UP"`
- Todos los componentes (db, redis, mail) en estado `UP`

**Cómo:** Postman / Insomnia / `curl` + captura de pantalla

### 5.4.4 Captura — Swagger UI operativo

**Qué capturar:** Navegador abierto en `https://tuevento-backend.onrender.com/swagger-ui` mostrando:
- El título "TuEvento API"
- La lista de controladores expandida
- Sin errores en consola DevTools (F12 → Console)

### 5.4.5 Captura — Migración exitosa en base de datos

**Qué capturar:** Resultado del query de verificación de Liquibase mostrando:
- Número total de tablas creadas (~45)
- Los últimos 5 changesets ejecutados con `exectype = EXECUTED`

---

# Apéndice A — Fases de Despliegue Futuras

| Fase | Componentes | Descripción |
|---|---|---|
| **Fase 1** ✅ | Backend + PostgreSQL + Redis + S3 | Este documento |
| **Fase 2** | Frontend Web (React/Nginx) | Static Site en Render, configurar `VITE_API_URL` |
| **Fase 3** | Fake Payment Gateway + nsfw-classifier | Servicios Docker adicionales en render.yaml |
| **Fase 4** | App móvil (React Native) | Build en Expo EAS, endpoints apuntan a Render |

---

# Apéndice B — Comandos de Diagnóstico Rápido

```bash
# Verificar health check
curl -s https://tuevento-backend.onrender.com/actuator/health

# Verificar que Swagger responde
curl -s -o /dev/null -w "%{http_code}" \
  https://tuevento-backend.onrender.com/swagger-ui/index.html

# Generar JWT Secret seguro
openssl rand -hex 32

# Generar Webhook Secret seguro
openssl rand -hex 32

# Conectar a PostgreSQL de Render
psql "postgresql://USER:PASS@HOST:5432/tuevento"

# Ver últimas migraciones Liquibase
psql "postgresql://USER:PASS@HOST:5432/tuevento" \
  -c "SELECT id, filename, dateexecuted FROM databasechangelog ORDER BY dateexecuted DESC LIMIT 10;"
```

---

# Apéndice C — Solución de Problemas Frecuentes

| Síntoma | Causa probable | Solución |
|---|---|---|
| Servicio en estado `Failed` | Error en build Docker o variable faltante | Revisar logs del deploy en Render |
| `Liquibase: Unable to acquire lock` | Deploy anterior se cayó a mitad de migración | `DELETE FROM databasechangeloglock;` en PostgreSQL |
| Redis `DOWN` en health check | SSL desactivado o credenciales incorrectas | Verificar `SPRING_REDIS_SSL=true` y password de Upstash |
| OAuth redirect_uri mismatch | URI no registrada en Google/Facebook Console | Agregar la URI de Render en la consola del proveedor |
| `ClassNotFoundException` al arrancar | Imagen Docker con Java 17 vs pom que compila con 17 pero usa features de 21 | El Dockerfile usa JRE 21 — verificar consistencia |
| Servicio se duerme (free tier) | Sin tráfico por 15 min | Normal en free tier; usar plan Starter para evitarlo |
| `UnsatisfiedDependencyException` | Variable de entorno requerida no configurada | Revisar qué bean falla y qué `${VAR}` le falta valor |
| S3 upload falla 403 | Credenciales incorrectas o bucket no existe | Verificar `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD` y nombre del bucket |
