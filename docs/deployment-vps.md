# Despliegue en VPS (Hostinger · Docker · Portainer CE · Nginx Proxy Manager)

Runbook para levantar TuEvento con `docker-compose.prod.yml` en un VPS Ubuntu 24.04
de 8 GB de RAM + 4 GB de swap. Los dominios son marcadores: reemplazar
`tuevento.example.com` por el dominio real en todo el documento y en `.env.prod`.

## 1. Topología

```
Internet ──► NPM :80/:443 (TLS Let's Encrypt)          red "edge"
              ├─ tuevento.example.com     ─► frontend:80 ──┬─ /      SPA (fallback a index.html)
              │                                            ├─ /api ─► backend:8080
              │                                            └─ /ws  ─► backend:8080 (WebSocket)
              ├─ s3.tuevento.example.com  ─► rustfs:9000   (URLs firmadas de imágenes)
              └─ pay.tuevento.example.com ─► fake-payment-gateway:4001 (popup de pago)

red "internal" (sin puertos publicados):
  backend ─► postgres, redis, rustfs, nsfw-classifier, fake-payment-gateway, libretranslate*
  fake-payment-gateway ─► postgres-gateway, backend (webhook)
```

`*` LibreTranslate solo arranca con el perfil `translate`.

Solo NPM publica puertos: 80 y 443 en todas las interfaces, y su panel (81) solo en
`127.0.0.1`. La API, Swagger y Actuator no tienen proxy host propio: el navegador
solo llega al backend por `/api` y `/ws`.

## 2. Imágenes `v1.0.0` (build y push)

Las 6 imágenes van al repositorio `jhampier23/tu-evento` de Docker Hub, diferenciadas
por prefijo de tag. `docker-compose.prod.yml` no usa `build:`; la pasarela simulada
también necesita su imagen porque la compra de tickets depende de ella.

| Servicio | Tag |
|---|---|
| backend | `jhampier23/tu-evento:v1.0.0` |
| frontend | `jhampier23/tu-evento:frontend-v1.0.0` |
| nsfw-classifier | `jhampier23/tu-evento:nsfw-v1.0.0` |
| fake-payment-gateway | `jhampier23/tu-evento:gateway-v1.0.0` |
| api-gateway (entrada de `/api` y `/ws`) | `jhampier23/tu-evento:api-gateway-v1.0.0` |
| storage-service (microservicio de archivos) | `jhampier23/tu-evento:storage-v1.0.0` |

Desde la raíz del repo, con `.env.prod` ya completo (solo se leen las `VITE_*`).
Si se construye en una máquina ARM (p. ej. Mac M1+), añadir `--platform linux/amd64`
a cada `docker build`.

```bash
docker login -u jhampier23

export $(grep -E '^VITE_' .env.prod | xargs)

docker build -t jhampier23/tu-evento:v1.0.0 TuEventoBackend/tu-evento

docker build -t jhampier23/tu-evento:frontend-v1.0.0 \
  --build-arg VITE_API_URL="$VITE_API_URL" \
  --build-arg VITE_GOOGLE_CLIENT_ID="$VITE_GOOGLE_CLIENT_ID" \
  --build-arg VITE_GATEWAY_URL="$VITE_GATEWAY_URL" \
  TuEventoFrontend/web

docker build -t jhampier23/tu-evento:nsfw-v1.0.0 nsfw-service

docker build -t jhampier23/tu-evento:gateway-v1.0.0 fake-payment-gateway

docker build -t jhampier23/tu-evento:api-gateway-v1.0.0 TuEventoBackend/gateway

# El contexto es services/ porque la imagen compila también tuevento-common
docker build -t jhampier23/tu-evento:storage-v1.0.0   -f TuEventoBackend/services/storage-service/Dockerfile TuEventoBackend/services

docker push jhampier23/tu-evento:v1.0.0
docker push jhampier23/tu-evento:frontend-v1.0.0
docker push jhampier23/tu-evento:nsfw-v1.0.0
docker push jhampier23/tu-evento:gateway-v1.0.0
docker push jhampier23/tu-evento:api-gateway-v1.0.0
docker push jhampier23/tu-evento:storage-v1.0.0
```

Build args de producción del frontend (Vite los incrusta en el bundle; cambiarlos
exige reconstruir la imagen):

| Variable | Valor |
|---|---|
| `VITE_API_URL` | `/api/v1` (relativa: mismo dominio, sin CORS; `/ws` se deriva de aquí) |
| `VITE_GOOGLE_CLIENT_ID` | Client ID OAuth web de Google |
| `VITE_GATEWAY_URL` | `https://pay.tuevento.example.com` |

### Publicar una versión: qué imágenes reconstruir y cuáles reetiquetar

`docker-compose.prod.yml` usa un único `TUEVENTO_VERSION` para las 6 imágenes, así que
**los 6 tags tienen que existir** con la versión nueva o el stack falla al hacer pull.

**Regla:** reconstruye las imágenes cuyo código cambió desde la versión anterior y
**reetiqueta solo las que no cambiaron**. Reetiquetar copia el manifiesto de la versión
anterior en Docker Hub (no descarga ni sube capas; el digest es el mismo). Construye
desde el commit de la versión (el tag `vX.Y.Z` en `main`, ver `docs/versioning.md`).

| Imagen | Tag | Carpeta que decide si cambió |
|---|---|---|
| backend | `vX.Y.Z` | `TuEventoBackend/tu-evento` |
| frontend | `frontend-vX.Y.Z` | `TuEventoFrontend/web` |
| gateway | `gateway-vX.Y.Z` | `fake-payment-gateway` |
| nsfw | `nsfw-vX.Y.Z` | `nsfw-service` |
| api-gateway | `api-gateway-vX.Y.Z` | `TuEventoBackend/gateway` |
| storage | `storage-vX.Y.Z` | `TuEventoBackend/services` |

Para cada carpeta, compara con el tag anterior; si no sale nada, esa imagen se reetiqueta:

```bash
for d in TuEventoBackend/tu-evento TuEventoFrontend/web fake-payment-gateway nsfw-service; do
  echo "== $d"; git diff --stat <tag-anterior>..HEAD -- "$d" | tail -1
done
```

> Las imágenes `v1.0.0` se construyeron desde el commit `98d1ad7`, que no tiene tag:
> úsalo como `<tag-anterior>` hasta que exista `v1.0.0` en git.

**Ejemplo: `v1.0.1`.** Desde `98d1ad7` cambian `TuEventoBackend/tu-evento`,
`TuEventoFrontend/web` y `fake-payment-gateway` (el sandbox de pagos, #100); `nsfw-service`
no cambia. Se reconstruyen backend, frontend y gateway, y solo se reetiqueta nsfw:

```bash
docker login -u jhampier23
export $(grep -E '^VITE_' .env.prod | xargs)

# 1. Reconstruir y publicar lo que cambió (una imagen a la vez)
docker build -t jhampier23/tu-evento:v1.0.1 TuEventoBackend/tu-evento
docker push jhampier23/tu-evento:v1.0.1

docker build -t jhampier23/tu-evento:frontend-v1.0.1 \
  --build-arg VITE_API_URL="$VITE_API_URL" \
  --build-arg VITE_GOOGLE_CLIENT_ID="$VITE_GOOGLE_CLIENT_ID" \
  --build-arg VITE_GATEWAY_URL="$VITE_GATEWAY_URL" \
  TuEventoFrontend/web
docker push jhampier23/tu-evento:frontend-v1.0.1

docker build -t jhampier23/tu-evento:gateway-v1.0.1 fake-payment-gateway
docker push jhampier23/tu-evento:gateway-v1.0.1

# 2. Reetiquetar la que no cambió, sin reconstruirla
docker buildx imagetools create -t jhampier23/tu-evento:nsfw-v1.0.1 jhampier23/tu-evento:nsfw-v1.0.0

# 3. Comprobar que existen los 4 tags antes de tocar el VPS
for t in v1.0.1 frontend-v1.0.1 gateway-v1.0.1 nsfw-v1.0.1; do
  docker manifest inspect jhampier23/tu-evento:$t > /dev/null && echo "OK  $t" || echo "FALTA  $t"
done
```

Después, en Portainer: cambiar `TUEVENTO_VERSION` a la versión nueva en las variables
del stack y **Update the stack** con *Re-pull image* activado. Para volver atrás basta
con restaurar el valor anterior de `TUEVENTO_VERSION` (las migraciones Liquibase no se
revierten solas: revisar el changelog de la versión antes de hacer rollback).

## 3. Preparar el VPS

1. DNS: registros `A` de `tuevento.example.com`, `s3.tuevento.example.com` y
   `pay.tuevento.example.com` apuntando a la IP del VPS.
2. Firewall (panel de Hostinger y/o UFW): permitir solo 22, 80 y 443.
3. Paneles de administración **solo en localhost**, accesibles por túnel SSH.

> **UFW no filtra los puertos publicados por Docker.** Docker inserta sus propias
> reglas de iptables (cadena `DOCKER`) antes que las de UFW, así que un
> `ports: "81:81"` queda abierto a internet aunque `ufw deny 81` esté activo. Por eso
> los paneles se publican en `127.0.0.1` y no se confía en UFW para ocultarlos.

### Panel de Nginx Proxy Manager (puerto 81)

El compose lo publica como `127.0.0.1:81:81`. Desde el equipo administrador:

```bash
ssh -L 8181:127.0.0.1:81 root@<IP-del-VPS>
# y abrir http://localhost:8181
```

### Portainer CE (puerto 9443)

Portainer se instala fuera de este stack. Publicarlo también solo en localhost
(si ya existe con `-p 9443:9443`, recrearlo: los datos quedan en el volumen):

```bash
docker rm -f portainer
docker run -d --name portainer --restart=always \
  -p 127.0.0.1:9443:9443 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v portainer_data:/data \
  portainer/portainer-ce:2.45.3
```

Acceso desde el equipo administrador:

```bash
ssh -L 9443:127.0.0.1:9443 root@<IP-del-VPS>
# y abrir https://localhost:9443
```

Comprobar desde fuera del VPS que los paneles no responden:
`curl -m 5 http://<IP-del-VPS>:81` y `curl -km 5 https://<IP-del-VPS>:9443` deben
fallar por timeout o conexión rechazada.

## 4. Desplegar el stack en Portainer

1. Copiar `.env.prod.example` a `.env.prod` en un equipo de confianza y reemplazar
   todos los valores (`openssl rand -base64 48` para los secretos). `.env.prod`
   está en `.gitignore`: nunca subirlo al repo.
2. Portainer → **Stacks → Add stack** → nombre `tuevento`.
3. **Repository**: URL del repo, referencia de la rama a desplegar y
   *Compose path* `docker-compose.prod.yml`. (Alternativa: *Web editor* pegando el archivo).
4. **Environment variables → Load variables from .env file** → subir `.env.prod`.
   Para levantar LibreTranslate añadir `COMPOSE_PROFILES=translate`.
5. **Deploy the stack**. El primer arranque del backend ejecuta las migraciones
   Liquibase (puede tardar 1–3 min; el healthcheck tiene `start_period` de 180 s).

Equivalente por CLI en el VPS:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
# con traducción:
docker compose -f docker-compose.prod.yml --env-file .env.prod --profile translate up -d
```

## 5. Proxy hosts en Nginx Proxy Manager

Panel en `http://localhost:8181` a través del túnel SSH (sección 3). En el primer
acceso, crear el usuario administrador o cambiar las credenciales por defecto.

En los 3 hosts: pestaña **SSL** → *Request a new SSL Certificate* (Let's Encrypt),
*Force SSL*, *HTTP/2 Support* y *HSTS Enabled*.

| Domain | Scheme | Forward host | Port | Websockets | Block common exploits |
|---|---|---|---|---|---|
| `tuevento.example.com` | http | `frontend` | 80 | **Sí** (`/ws`) | Sí |
| `s3.tuevento.example.com` | http | `rustfs` | 9000 | No | No (no filtrar la query de las URLs firmadas) |
| `pay.tuevento.example.com` | http | `fake-payment-gateway` | 4001 | No | Sí |

NPM resuelve los nombres de servicio porque comparte la red `tuevento-edge`.
No sobrescribir la cabecera `Host` en `s3.`: la firma S3 V4 incluye el host y debe
coincidir con `RUSTFS_PUBLIC_URL`.

### Login social (Google y Facebook)

`APP_OAUTH_GOOGLE_REDIRECT_URI` y `APP_OAUTH_FACEBOOK_REDIRECT_URI` son la **URL completa
del callback**, no solo el dominio. El backend usa ese valor tal cual como `redirect_uri`
en el diálogo del proveedor y en el canje del code; si no coincide carácter por carácter
con lo registrado, el proveedor rechaza el login.

| Variable | Valor | Dónde registrarlo |
|---|---|---|
| `APP_OAUTH_FACEBOOK_REDIRECT_URI` | `https://tuevento.example.com/api/v1/auth/oauth/facebook/callback` | Meta for Developers → Facebook Login → *Valid OAuth Redirect URIs* |
| `APP_OAUTH_GOOGLE_REDIRECT_URI` | `https://tuevento.example.com/api/v1/auth/oauth/google/callback` | Google Cloud Console → *Authorized redirect URIs* (la web entra con Google por GSI, que además exige el dominio en *Authorized JavaScript origins*) |
| `APP_OAUTH_ALLOWED_FRONTEND_REDIRECT_URIS` | `https://tuevento.example.com/login` | — (lista blanca del redirect final al frontend) |

En local (perfil `dev`), sin las variables, los callbacks por defecto son
`http://localhost:8080/api/v1/auth/oauth/{google|facebook}/callback`.

Si el usuario cancela en el proveedor o el login se rechaza (cuenta bloqueada,
desactivada…), el backend redirige a `/login?oauth_error=<código>` y la web muestra el
mensaje correspondiente.

## 6. Cómo se sirven las imágenes (`/storage/{id}/url`)

`GET /api/v1/storage/{id}/url` → `GeneratePublicUrlUseCase` → `S3Presigner` firmado
con `app.storage.rustfs.public-url` (variable **obligatoria** `RUSTFS_PUBLIC_URL`; si
quedara vacía, las URLs saldrían con el host interno `http://rustfs:9000` y las
imágenes no cargarían en el navegador). Devuelve una URL path-style válida 60 min:

```
https://s3.tuevento.example.com/tuevento/event_media/<uuid>.jpg?X-Amz-Algorithm=...&X-Amz-Signature=...
```

La firma se calcula en el backend sin llamar a RustFS; el **navegador** descarga
el archivo directamente de ese host, por eso `s3.` necesita proxy host público.
Las subidas y borrados van por la red interna (`RUSTFS_ENDPOINT=http://rustfs:9000`).
La consola de RustFS está desactivada.

**Control de acceso.** Todo `/api/v1/storage` exige sesión. Las imágenes de eventos se
sirven por la URL guardada en `event_media.img_url`, sin pasar por este endpoint. El caso de
uso solo deja leer un archivo privado (`ORGANIZER_DOCUMENT`) a quien lo subió o a un ADMIN
(403 en otro caso); por REST solo se suben fotos de perfil y los documentos de organizador
solo los borra un ADMIN.

**URL pública guardada.** Desde `fix/storage-access-control`, `stored_file.public_url` y
`event_media.img_url` se guardan con `RUSTFS_PUBLIC_URL`, y los metadatos de
`GET /storage/{id}` calculan la URL al leer. Las filas de `event_media` creadas antes
siguen con el host interno; para corregirlas una sola vez (con respaldo previo):

```bash
docker exec -it tuevento-postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "
  UPDATE event_media     SET img_url    = replace(img_url,    'http://rustfs:9000', 'https://s3.tuevento.example.com') WHERE img_url    LIKE 'http://rustfs:9000/%';
  UPDATE event_media_log SET img_url    = replace(img_url,    'http://rustfs:9000', 'https://s3.tuevento.example.com') WHERE img_url    LIKE 'http://rustfs:9000/%';
  UPDATE stored_file     SET public_url = replace(public_url, 'http://rustfs:9000', 'https://s3.tuevento.example.com') WHERE public_url LIKE 'http://rustfs:9000/%';"
```

## 7. Smoke tests tras el despliegue

### 7.1 Contenedores y backend

```bash
# Estado de los contenedores (todos healthy)
docker compose -f docker-compose.prod.yml --env-file .env.prod ps

# Health interno del backend (Actuator no se publica)
docker exec tuevento-backend wget -qO- http://localhost:8080/actuator/health   # {"status":"UP"}
docker exec tuevento-backend sh -c 'wget -S -qO- http://localhost:8080/actuator 2>&1 | grep HTTP/'   # 404
```

### 7.2 Desde fuera del VPS

```bash
curl -I https://tuevento.example.com/                       # 200 text/html
curl -I https://tuevento.example.com/events/1               # 200 (fallback SPA)
curl -s https://tuevento.example.com/api/v1/events/public   # JSON {"success":true,...}
curl -s https://tuevento.example.com/ws/info                # {"websocket":true,...}
curl -I https://pay.tuevento.example.com/pay.html           # 200
curl -m 5 http://<IP-del-VPS>:81                            # timeout / rechazado
```

### 7.3 Flujo en el navegador

Login (correo y Google), subir una imagen a un evento (debe cargar desde `s3.`),
compra de ticket con el popup de `pay.`, y que lleguen notificaciones en tiempo real.

### 7.4 Evidencia de Swagger (temporal)

Swagger está desactivado en producción (`SWAGGER_ENABLED=false`) y, aunque se active,
no tiene proxy host: solo se alcanza desde la red interna. Para capturar la evidencia:

1. En Portainer, poner `SWAGGER_ENABLED=true` en las variables del stack y pulsar
   *Update the stack* (solo se recrea el backend).
2. En el VPS, abrir un reenvío temporal en localhost hacia el backend:
   ```bash
   docker run --rm -d --name swagger-tunnel --network tuevento-internal \
     -p 127.0.0.1:8088:8080 alpine/socat:1.8.0.3 \
     tcp-listen:8080,fork,reuseaddr tcp-connect:backend:8080
   ```
3. Desde el equipo administrador: `ssh -L 8088:127.0.0.1:8088 root@<IP-del-VPS>` y
   abrir `http://localhost:8088/swagger-ui/index.html`. Tomar las capturas.
4. Cerrar el reenvío: `docker rm -f swagger-tunnel`.
5. **Volver a desactivarlo**: `SWAGGER_ENABLED=false` → *Update the stack*, y comprobar
   que `docker exec tuevento-backend sh -c 'wget -S -qO- http://localhost:8080/api-docs 2>&1 | grep HTTP/'`
   devuelve 404.

Google Cloud Console: origen autorizado `https://tuevento.example.com` y redirect
`https://tuevento.example.com/api/v1/auth/oauth/google/callback` (igual para Facebook).

La app móvil debe apuntar a `https://tuevento.example.com/api/v1`.

## 8. Operación

- **Actualizar versión**: construir y publicar con el nuevo tag (sección 2), cambiar
  `TUEVENTO_VERSION` en las variables del stack y pulsar *Update the stack* con
  *Re-pull image*.
- **Backup de la BD**:
  `docker exec tuevento-postgres pg_dump -U <POSTGRES_USER> -Fc <POSTGRES_DB> > tuevento-$(date +%F).dump`
- **Volúmenes** a respaldar: `tuevento_postgres_data`, `tuevento_rustfs_data`,
  `tuevento_npm_data`, `tuevento_npm_letsencrypt`.
- **Logs**: rotados por Docker (3 × 10 MB por contenedor).

### Límites de memoria

| Servicio | Límite | Notas |
|---|---|---|
| backend | 1280 MB | `-Xms256m -Xmx768m -XX:MaxMetaspaceSize=256m` |
| nsfw-classifier | 1536 MB | TensorFlow |
| postgres | 768 MB | |
| rustfs | 512 MB | |
| fake-payment-gateway | 512 MB | `-Xmx256m` |
| npm | 256 MB | |
| postgres-gateway | 256 MB | |
| redis | 192 MB | `maxmemory 128mb`, sin persistencia |
| storage-service | 512 MB | `-Xmx320m` |
| api-gateway | 384 MB | `-Xmx256m` |
| frontend | 64 MB | |
| libretranslate (opcional) | 2048 MB | perfil `translate` |

Total ≈ 6.3 GB sin LibreTranslate y ≈ 8.3 GB con él (topes, no reservas; con
LibreTranslate el swap absorbe la diferencia y los picos puntuales).
