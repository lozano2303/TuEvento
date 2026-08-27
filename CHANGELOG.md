# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Changed - Scheduler Frequency Increased + Optimistic Expiration (Backend + Web + Mobile)
- **Backend**: frecuencia del scheduler de expiración aumentada de 60s a 10s
  - **SeatReservationExpirationScheduler.java**: cron cambiado de `0 * * * * *` (cada minuto) a `*/10 * * * * *` (cada 10 segundos)
  - **Motivo**: TTL de 10 minutos se sentía lento al expirar (demora de hasta 59s), ahora es casi instantáneo (máximo 10s de demora desde el backend)
  - **Optimización de performance**: agregado índice compuesto en `(status, reserved_until)` para soportar consultas frecuentes sin impacto
    - **062-add-seat-expiration-index.yaml**: `CREATE INDEX idx_seat_status_reserved_until ON seat(status, reserved_until)`
    - Query del scheduler: `WHERE status='RESERVED' AND reserved_until < NOW()` ahora usa el índice
    - Sin el índice, correr esto cada 10s causaría table scans costosos
- **Frontend (web + móvil)**: deselección optimista al expirar countdown
  - **CartItem (ambas plataformas)**: cuando `diffMs <= 0`, llama a `onExpire(seatId)` inmediatamente
    - **Antes**: mostraba "Expirado" pero la silla permanecía en el carrito hasta que el scheduler la liberaba (hasta 59s de demora)
    - **Ahora**: la silla desaparece del carrito y se deselecciona del mapa al instante cuando el countdown llega a 0
  - **EventDetail.jsx (web) + EventDetailScreen.jsx (móvil)**: nuevo handler `handleSeatExpire`
    - **Solo actualiza estado local** (no hace petición HTTP al backend)
    - Cambia `status='AVAILABLE'`, `reservedBy=null`, `reservedUntil=null`
    - El scheduler del backend confirma la liberación en su próximo ciclo (cada 10s)
    - Si llega el evento de WebSocket después, no duplica ni rompe nada (la silla ya está liberada localmente)
  - **UX mejorada**: la expiración se siente instantánea para el usuario
    - Countdown llega a 0 → silla desaparece del carrito inmediatamente
    - Mapa actualiza el color de la silla de verde/amarillo a blanco (disponible)
    - Máximo 10s de demora para que otros usuarios vean la silla disponible (vs. 60s antes)

### Fixed - Stepper Hydration: Restore Reserved Seat Count on View Re-entry
- **EventDetail.jsx (web) + EventDetailScreen.jsx (móvil)**: hidratación del stepper con sillas ya reservadas
  - **Bug**: al salir y volver a la vista de selección de sillas, el stepper/selector de cantidad siempre mostraba 1, aunque el usuario tuviera 2+ sillas ya reservadas (visibles en el mapa y carrito)
  - **Causa**: `useState(1)` inicializa el stepper en 1 fijo, sin considerar sillas previamente reservadas
  - **Solución**: `useEffect` que detecta cuando `cart.length > selectedQuantity` y actualiza el stepper
    ```javascript
    useEffect(() => {
      if (cart.length > 0 && cart.length > selectedQuantity) {
        setSelectedQuantity(cart.length);
      }
    }, [cart.length]);
    ```
  - **Flujo**:
    1. Usuario entra a la vista → se cargan sillas del backend
    2. `cart` (computed) filtra sillas con `reservedBy === currentUserId`
    3. Si `cart.length > selectedQuantity` (ej. 3 > 1) → stepper sube a 3
    4. Si el usuario no tiene sillas reservadas → stepper queda en 1 (comportamiento original)
  - **Caso límite (expiración)**: 
    - Usuario tenía 3 sillas, pero 1 expiró mientras no estaba en la vista
    - Al entrar: backend retorna solo 2 con `reserved_until` vigente
    - Stepper se hidrata con 2 (no con 3)
  - **Consistencia con validación existente**: 
    - El stepper ya bloqueaba decrementar por debajo de `cart.length`
    - Ahora también **inicializa** en `cart.length` si es mayor a 1
    - Evita estado inconsistente donde el botón - está bloqueado pero el número dice 1
  - **Aplicado en ambas plataformas**: web y móvil con lógica idéntica

### Added - Cart with TTL Countdown on Mobile
- **EventDetailScreen.jsx → CartItem component**: countdown de TTL para reservas de sillas
  - **Componente CartItem**: item individual del carrito con información de silla y countdown
    - Código de silla (ej. "A1", "B2")
    - Nombre de sección (ej. "VIP", "General")
    - Precio formateado (ej. "$25.00")
    - **Countdown en tiempo real**: formato `mm:ss` (ej. "9:47", "0:23")
      - Calcula tiempo restante desde `reservedUntil` (ISO timestamp)
      - Actualización cada 1 segundo con `setInterval`
      - Muestra "Expirado" cuando `diffMs <= 0`
    - Botón de liberar silla (ícono X rojo)
      - Deshabilitado cuando `isReleasing` (optimistic UI)
  - **UI del carrito**: panel colapsable con lista de sillas reservadas
    - Header: ícono carrito + "Tus Sillas" + contador `({cart.length})`
    - FlatList con `scrollEnabled={false}` (altura máxima 200px)
    - Se muestra solo si `cart.length > 0`
    - Ubicación: después del selector de cantidad, antes del menú de secciones
  - **Manejo de expiración automática**: 
    - Backend: `SeatReservationExpirationScheduler` corre cada minuto
    - Emite `SeatStatusChangedEvent` con `changedBy=null` (liberación automática)
    - WebSocket propaga evento a todos los clientes conectados
    - Listener móvil actualiza estado: `status='AVAILABLE'`, `reservedBy=null`, `reservedUntil=null`
    - Silla desaparece del carrito automáticamente (react a cambio de `cart` computed)
  - **Estilos**: consistentes con diseño de EventDetailScreen
    - `cartContainer`: fondo `colors.surface`, borde `colors.primary + "20"`
    - `cartItem`: fondo `colors.primary + "10"`, padding 10px, border radius 8px
    - `cartItemTimer`: ícono reloj + texto pequeño gris
    - `cartItemRemove`: ícono `close-circle` rojo, opacidad 0.5 cuando disabled
  - **Optimización**: un solo interval por item (no compartido entre items)
    - Cleanup automático con `return () => clearInterval(interval)` en useEffect
    - Dependencia: `[seat.reservedUntil]` → recrea interval solo si cambia el timestamp

### Fixed - Quantity Stepper Validation: Cannot Decrease Below Selected Seats
- **EventDetail.jsx → SeatSelectorSection**: fix de validación del stepper de cantidad
  - **Bug**: el botón - permitía bajar la cantidad por debajo de las sillas ya seleccionadas/reservadas
    - Ejemplo: cantidad 5, 5 sillas seleccionadas → botón - permitía bajar a 4, dejando inconsistencia
  - **Solución**: validación en `handleQuantityDecrease`
    ```javascript
    if (selectedQuantity > 1 && selectedQuantity > cart.length) {
      setSelectedQuantity(selectedQuantity - 1);
    }
    // Bloqueado cuando selectedQuantity === cart.length
    ```
  - **Feedback visual**: botón - deshabilitado cuando `selectedQuantity <= cart.length`
    - `disabled={selectedQuantity <= 1 || selectedQuantity <= cart.length}`
    - Tooltip: "No puedes bajar de N (sillas ya seleccionadas)" cuando aplica
  - **Comportamiento**:
    - Cantidad 5, 4 sillas seleccionadas → permite bajar hasta 4 (bloqueado en 4)
    - Cantidad 5, 5 sillas seleccionadas → botón - completamente bloqueado
    - Liberar una silla → botón - se habilita de nuevo (permite bajar un paso)
  - **UX**: evita inconsistencia entre cantidad solicitada y sillas actualmente en carrito

### Fixed - Canvas Real Dimensions + Improved Layout Proportions
- **EventDetail.jsx → SeatSelectorSection**: fix crítico de dimensiones del canvas
  - **Problema**: `calculateFraming` usaba dimensiones hardcodeadas (900×600) que no coincidían con el espacio real del contenedor
  - **Causa**: Layout de 3 columnas dejaba menos ancho disponible para el canvas, causando recorte de contenido ("General"/"VIP" fuera de borde)
  - **Solución**: dimensiones reales del contenedor con ResizeObserver + getBoundingClientRect
    - Estado `containerSize` { width, height } actualizado dinámicamente
    - `useEffect` observa cambios de tamaño del contenedor (ResizeObserver)
    - Listener de `window.resize` como fallback
    - `calculateFraming` recibe `viewportWidth` y `viewportHeight` como parámetros (no constantes)
    - Stage con `width={containerSize.width}` y `height={containerSize.height}` (dinámico)
  - **Recalculo automático**: el encuadre se recalcula al montar, al redimensionar ventana, y al cambiar de sección
  - **Centrado preciso**: offset (x, y) calculado del centro real del contenido y viewport dinámico
- **Layout de 3 columnas rebalanceado**:
  - **Izquierda - Menú de secciones**: `w-56` (224px, antes 256px) + `shrink-0` (ancho fijo)
    - Más compacto, solo lista de secciones
    - Texto "disponibles" abreviado a disponibilidad numérica (ej. "20/50")
  - **Centro - Mapa**: `flex-1` + `minWidth: 0` (flexible, dominante)
    - Ocupa todo el espacio restante entre los paneles laterales
    - `minWidth: 0` previene overflow en contenedores flex
  - **Derecha - Carrito**: `w-72` (288px, antes 320px) + `shrink-0` (ancho fijo)
    - Panel visualmente separado con borde claro
    - Tamaño óptimo para mostrar items del carrito sin scroll excesivo
  - **Gap**: 16px (`gap-4`) entre columnas para separación visual clara

### Changed - Guided View System: Overview → Zoom to Selected Section
- **EventDetail.jsx → SeatSelectorSection**: reemplazado pan libre por sistema de vista guiada con animaciones automáticas
  - **Import explícito de Konva**: agregado `import Konva from 'konva'` en lugar de depender de `window.Konva`
    - Uso correcto: `new Konva.Tween(...)` y `Konva.Easings.EaseInOut`
    - Más robusto y type-safe, evita dependencia de globals
  - **Vista general (overview)**: todas las secciones visibles, centradas automáticamente con zoom-out
    - Zoom y posición calculados dinámicamente del AABB completo del layout usando `getElementAABB`
    - Secciones clickeables con cursor pointer, borde destacado (opacity: 0.5, stroke más grueso)
    - Sillas NO clickeables en este estado — solo navegación de secciones
  - **Vista de sección**: zoom automático y centrado en la sección seleccionada
    - Animación suave (Konva.Tween, 500ms, EaseInOut) desde overview hacia la sección
    - Sillas de la sección clickeables para seleccionar/deseleccionar
    - Stage completamente estático — sin pan manual, sin zoom manual, sin scrollbars
  - **Transiciones bidireccionales**: 
    - Click en sección (mapa o menú lateral) → anima hacia esa sección
    - Botón "Ver todas" → anima de regreso al overview
  - **Cálculo de encuadre**: función `calculateFraming(elements)` reutiliza lógica de AABB
    - Calcula scale óptimo para que el contenido entre con margen (80px)
    - Calcula offset (x, y) para centrar el contenido en el viewport (900×600)
    - Máximo 2x zoom para evitar pixelación
  - **`animateToFraming(framing, duration)`**: wrapper de Konva.Tween para animar Stage
    - Anima simultáneamente scaleX, scaleY, x, y
    - Sincroniza estado de zoom con el valor final de la animación
  - **useEffect con `selectedSectionFilter`**: trigger automático de animaciones
    - `null` → vista general
    - `sectionId` → vista de esa sección
- **SectionRenderer**: lógica de interacción según modo de vista
  - **`inOverviewMode`**: determina si las secciones son clickeables (sillas ocultas/no clickeables)
  - **`onSectionClick`**: callback para seleccionar sección desde el mapa
  - Fondo de sección con `listening={inOverviewMode}` y `cursor="pointer"` solo en overview
  - Sillas renderizadas solo cuando `!inOverviewMode`
- **Eliminados**:
  - Controles de zoom manual (+/- buttons)
  - Pan con clic derecho (handleStageMouseDown, handleStageMouseMove, handleStageMouseUp, handleContextMenu)
  - Estado `stagePos`, `panState`, `panLimits`, `clampPosition`
  - Props del Stage: `onMouseDown`, `onMouseMove`, `onMouseUp`, `onMouseLeave`, `onContextMenu`
  - `overflow: auto` en container (ahora `overflow: hidden` — sin scrollbars)
- **Selector de cantidad, carrito, WebSocket**: sin cambios — siguen funcionando igual

### Technical Details - Guided View System
- **Konva.Tween**: animación nativa de Konva para transiciones suaves entre estados
- **AABB calculation**: reutiliza `getElementAABB` de layoutEditorUtils para calcular bounding boxes
- **Viewport fixed**: 900×600px, zoom y posición calculados para ajustar contenido a estas dimensiones
- **Zoom margin**: 80px de padding alrededor del contenido para evitar bordes cortados
- **useEffect dependency**: recalcula y anima automáticamente al cambiar `selectedSectionFilter`
- **Bidirectional flow**: overview ⇄ section con la misma lógica de animación

### Added - Pan Navigation with Right-Click in Seat Selector
- **OBSOLETO — Reemplazado por Guided View System**
- ~~Pan con clic derecho y límites dinámicos~~ → ahora vista guiada automática sin interacción manual de cámara

### Changed - Integrated Seat Selector into EventDetail
- **EventDetail.jsx**: integración completa del selector de sillas dentro de la página de detalle del evento
  - **Selector de cantidad estilo cine**: stepper +/- para elegir cantidad de sillas (1-10) antes de interactuar con el mapa
  - **Menú lateral de secciones**: lista todas las secciones del evento con nombre, disponibilidad y precio
    - Click en sección → activa filtro (solo esa sección es seleccionable en el mapa)
    - Botón "Ver todas" para desactivar el filtro
  - **Canvas visual con Konva**: mapa interactivo de sillas reutilizando geometría de `distributeSeats`
    - Controles de zoom +/- (30% - 200%)
    - Sillas filtradas se muestran atenuadas y no son clickeables
  - **Carrito lateral integrado**:
    - Muestra código de silla, nombre de sección y precio individual
    - Countdown en tiempo real del TTL (MM:SS)
    - Total acumulado de todas las sillas
    - Botón "Continuar al Pago" (placeholder)
  - **Validación de cantidad**: bloquea reservas adicionales si se alcanza el límite elegido
    - Muestra alert: "Ya seleccionaste N silla(s). Cambia la cantidad si necesitas más."
  - **WebSocket en tiempo real**: sincronización automática entre pestañas/usuarios
  - **Componentes auxiliares**:
    - `SeatSelectorSection`: contenedor principal con layout completo
    - `SectionMenu`: menú de secciones con filtrado
    - `SectionRenderer`: geometría de sección (rect/polygon) + sillas
    - `SeatCircle`: círculo individual con lógica de color, estado y click
    - `CartPanel`: panel lateral con carrito y total
    - `CartItem`: item individual con countdown y precio
- **App.jsx**: eliminada ruta `/events/:eventId/select-seats` (ya no es necesaria)
  - Actualizado regex de `showNavbar` para remover patrón de select-seats
  - Eliminado import de `EventSeatSelector`
- **EventSeatSelector.jsx**: archivo eliminado (funcionalidad movida a EventDetail)

### Technical Details - Seat Selector Integration
- Estado unificado en EventDetail: layout, secciones, sillas, WebSocket, carrito
- `selectedQuantity`: límite máximo de sillas que el usuario puede reservar (validado en `handleReserveSeat`)
- `selectedSectionFilter`: ID de sección activa para filtrar (null = todas visibles)
- Sillas fuera de sección filtrada: `opacity: 0.3`, no clickeables
- Carrito: derivado con `useMemo` de `seats` filtrado por `reservedBy === currentUserId`
- Precios: obtenidos de `EventSectionService.getByEvent()` y mapeados por `eventSectionId`

### Fixed - SockJS Global Polyfill for Vite
- **vite.config.js**: agregado `define: { global: 'globalThis' }` para compatibilidad con `sockjs-client`
  - Problema: `sockjs-client` asume entorno Webpack/Node donde `global` existe implícitamente
  - Vite no define `global` → ReferenceError que tumba todo el bundle
  - Solución: reemplazar `global` por `globalThis` (estándar moderno de navegadores) durante build
  - Requiere reinicio completo del dev server (hot-reload no es suficiente)

### Added - EventSeatSelector Component (Interactive Seat Purchase View)
- **EventSeatSelector.jsx**: componente completo de selección de sillas para compradores
  - Carga layout visual del evento + estado real de sillas vía API
  - Renderiza secciones con Konva (reutiliza lógica de `distributeSeats`)
  - WebSocket en tiempo real para actualizaciones de estado de sillas
  - **Colores por estado**:
    - Verde: `AVAILABLE` (clickeable)
    - Azul: `RESERVED` por usuario actual (clickeable para liberar)
    - Amarillo: `RESERVED` por otro usuario (no clickeable)
    - Gris: `SOLD` (no clickeable)
    - Morado: `COURTESY` (no clickeable)
  - **Interacción**:
    - Click en silla verde → reserva temporal (10 min)
    - Click en silla azul (propia) → libera reserva
    - Manejo de estados de carga (reserving/releasing)
    - Refrescado automático en caso de race condition
  - **Carrito lateral**:
    - Lista de sillas reservadas por el usuario
    - Countdown en tiempo real del TTL (MM:SS)
    - Botón para liberar cada silla individual
    - Botón "Continuar al Pago" (placeholder, no implementado)
  - **Ruta**: `/events/:eventId/select-seats` (pública para ver, auth para reservar)
  - Sin navbar (full-screen experience)
- **Componentes internos**:
  - `SectionRenderer`: renderiza geometría de sección (rect/polygon) + sillas
  - `SeatCircle`: círculo individual con lógica de color y click
  - `CartPanel`: panel lateral con carrito y countdown
  - `CartItem`: item individual con temporizador
- **Zoom**: controles +/- para ajustar vista del canvas (30% - 200%)

### Technical Details - EventSeatSelector
- Usa `connectSeatSocket` para recibir actualizaciones en tiempo real
- Actualización puntual de estado al recibir evento WebSocket (no recarga completa)
- Estado local: `{ [seatId]: SeatResponse }` para acceso O(1)
- Carrito derivado con `useMemo` filtrando por `reservedBy === currentUserId`
- Detección de usuario no autenticado: redirige a `/login` al intentar reservar
- Cleanup de WebSocket en `useEffect` para evitar memory leaks
- Manejo de errores: muestra mensaje y refresca estado de silla en caso de fallo

### Changed - App.jsx Routes
- Nueva ruta `/events/:eventId/select-seats` sin ProtectedRoute (pública)
- Regex actualizado en `showNavbar` para ocultar navbar en selector de sillas

### Added - Frontend WebSocket Client and Seat Services
- **SeatService.js**: servicios HTTP para gestión de sillas
  - `getSeatsBySection(eventSectionId)`: GET público, no requiere auth
  - `reserveSeat(seatId)`: POST con auth, reserva temporal de 10 minutos
  - `releaseSeat(seatId)`: POST con auth, libera reserva antes de expiración
  - Usa `httpRequest` con auto-refresh de JWT en 401
- **websocketClient.js**: cliente WebSocket para actualizaciones en tiempo real
  - `connectSeatSocket(eventId, onSeatUpdate)`: conecta vía STOMP + SockJS
  - Suscripción a `/topic/events/{eventId}/seats`
  - Token JWT en query param del handshake (`/ws?token=...`)
  - Reconexión automática cada 5 segundos si se pierde conexión
  - Heartbeat cada 10 segundos para detectar conexiones muertas
  - **Estrategia de auth**: si no hay token, no conecta WebSocket (usuarios anónimos ven estado estático)
- **Dependencias instaladas**: `@stomp/stompjs`, `sockjs-client`
- Debug logging solo en desarrollo (`import.meta.env.DEV`)

### Technical Details - WebSocket
- `WS_BASE_URL`: deriva de `VITE_API_URL` quitando `/api/v1` (ej: `http://localhost:8080`)
- SockJS negocia transporte automáticamente (WebSocket nativo, polling, streaming)
- Callback `onSeatUpdate` recibe: `{ seatId, eventId, eventSectionId, oldStatus, newStatus, changedBy, reservedUntil }`
- `changedBy = null` indica cambio automático del sistema (expiración de reserva)
- Cliente retorna `null` si no hay token (permite lógica condicional en componentes)

### Fixed - Seat Constraints Case Sensitivity
- **Changeset 061**: corrige `chk_seat_type` y `chk_seat_status` para usar MAYÚSCULAS
  - Problema: changeset 056 original creó constraints con minúsculas (`'available'`, `'reserved'`, etc.)
  - JPA con `@Enumerated(EnumType.STRING)` envía nombres de enum en MAYÚSCULAS (`'AVAILABLE'`, `'RESERVED'`, etc.)
  - Resultado: inserts violaban la constraint → `ERROR: new row for relation "seat" violates check constraint "chk_seat_status"`
- **chk_seat_type**: ahora acepta `'REGULAR'`, `'COURTESY'` (antes: `'regular'`, `'courtesy'`)
- **chk_seat_status**: ahora acepta `'AVAILABLE'`, `'RESERVED'`, `'SOLD'`, `'COURTESY'` (antes: minúsculas)
- No requiere migración de datos porque la tabla `seat` estaba vacía

### Added - Automatic Seat Generation from Layout
- **SaveEventLayoutService**: generación automática de `seat_block` y `seat` al guardar el layout
  - Parsea `layoutData` JSON y extrae cada sección con `seatLayout: { targetSeats, rows, seatsPerRow }`
  - Por cada sección con `backendSectionId`:
    - Borra sillas existentes de esa sección (validando que estén AVAILABLE)
    - Borra seat_blocks existentes
    - Crea un nuevo seat_block con capacity=targetSeats
    - Genera targetSeats registros de seat con códigos legibles (A1, A2, B1, B2...)
  - Todas las sillas se crean con `status=AVAILABLE`, `type=REGULAR`
  - Corre en la misma transacción que guarda el EventLayout
- **Validación defensiva**: si alguna silla tiene `status != AVAILABLE`, lanza `SEAT_REGENERATION_CONFLICT` en lugar de borrarla
- **Logging**: reporta cantidad de secciones procesadas y sillas generadas por sección
- **Algoritmo de códigos**: fila como letra (A, B, C... Z, AA, AB...), posición como número (1, 2, 3...)

### Changed
- `SaveEventLayoutService`: ahora inyecta `SeatBlockRepository`, `SeatRepository` y `ObjectMapper`
- Frontend no necesita cambios — la generación es automática en backend

### Technical Details
- Regeneración completa por sección: borra y recrea todas las sillas cada vez que se guarda el layout
- Estrategia simple: un seat_block por sección (nombre "Bloque Principal")
- Si el layout cambia targetSeats, las sillas se ajustan automáticamente
- Secciones sin `seatLayout` o sin `backendSectionId` se saltan sin error

### Added - Seat Reservation System with TTL + WebSocket
- **Seat Reservation with TTL**: Sistema completo de reservas temporales de sillas con expiración automática
  - `SeatReservationService`: servicio de aplicación con `reserveSeat()` y `releaseSeat()`
  - TTL de 10 minutos configurable en `RESERVATION_TTL_MINUTES`
  - Reservas incluyen `reserved_by` (userId) y `reserved_until` (LocalDateTime)
- **Automatic Expiration Scheduler**: 
  - `SeatReservationExpirationScheduler`: libera automáticamente sillas cuyo `reserved_until` expiró
  - Cron: ejecuta cada minuto (`0 * * * * *`)
  - Catch-up: se ejecuta al iniciar la aplicación vía `@EventListener(ApplicationReadyEvent)`
  - Self-injection con `@Lazy` para que `@Transactional` funcione correctamente
  - Logs a nivel INFO para visibilidad en producción
- **WebSocket Real-time Notifications**:
  - `SeatStatusWebSocketListener`: transmite cambios de estado de sillas en tiempo real
  - Topic: `/topic/events/{eventId}/seats` (routing por evento)
  - Usa `SimpMessagingTemplate` para broadcast a todos los clientes conectados
- **REST Endpoints**:
  - `POST /api/v1/seats/{seatId}/reserve`: reservar silla (cualquier usuario autenticado)
  - `POST /api/v1/seats/{seatId}/release`: liberar reserva (solo dueño o ADMIN)
  - Ambos endpoints protegidos con `@PreAuthorize("isAuthenticated()")`
- **Domain Event Extension**:
  - `SeatStatusChangedEvent`: agregados campos `eventId` (para routing WebSocket) y `reservedUntil` (para informar TTL a clientes)
- **Repository Extensions**:
  - `SeatRepository.findAllByStatusAndReservedUntilBefore()`: query para buscar sillas expiradas
  - Implementación en `SeatRepositoryImpl` y `SeatJpaRepository`
- **Test utilities**:
  - `test-seat-reservation.sh`: script bash para pruebas de API
  - `websocket-test-client.html`: cliente WebSocket de prueba con interfaz gráfica
  - `SEAT_RESERVATION_IMPLEMENTATION.md`: documentación completa con plan de verificación

### Changed
- `SeatStatusChangedEvent`: agregados campos `eventId` y `reservedUntil`
- `SeatRepository`: agregado método `findAllByStatusAndReservedUntilBefore()`

### Security
- Solo el usuario que reservó una silla puede liberarla manualmente (excepto ADMIN)
- Validación de estado de silla antes de reservar (debe ser AVAILABLE)
- `changedBy = null` en eventos indica cambio automático del sistema

### Technical Details
- Scheduler sigue el patrón de `EventAutoCompletionScheduler` (catch-up + cron)
- WebSocket usa la configuración existente en `WebSocketConfig`
- Obtiene `eventId` desde `EventSection` para routing correcto de topics
- Build verificado: `BUILD SUCCESS` sin errores de compilación

### Added
- Category y CategoryEvent: modelos de dominio puros
- CategoryRepository y CategoryEventRepository: interfaces de dominio
- CategoryCreatedEvent, CategoryDeactivatedEvent, CategoryAssignedToEventEvent: eventos de dominio
- CategoryEntity, CategoryEventEntity: entidades JPA
- CategoryJpaRepository, CategoryEventJpaRepository: repositorios Spring Data
- CategoryRepositoryImpl, CategoryEventRepositoryImpl: implementaciones de dominio
- CategoryEntityMapper, CategoryEventEntityMapper: mappers de infraestructura
- CategoryUseCase, CategoryEventUseCase: ports de entrada
- CreateCategoryRequest, UpdateCategoryRequest, AssignCategoryRequest: DTOs de request con validaciones Jakarta
- CategoryResponse, CategoryEventResponse: DTOs de respuesta inmutables
- CategoryUseCaseImpl, CategoryEventUseCaseImpl: casos de uso con publicación de eventos de dominio
- CategoryMapper: mapper de aplicación
- CategoryController: 9 endpoints REST en /api/v1/categories
- CategoryEventController: 5 endpoints REST en /api/v1/category-events
- SecurityConfig actualizado con permisos por rol para el módulo category

### Changed
- CreateEventRequest: campo categoryId obligatorio (@NotNull Integer)
- CreateEventService: valida categoría activa y asigna automáticamente en category_event al crear un evento

### Fixed
- Ya no es posible crear un evento sin categoría
- CreateEventService: validación de existencia de siteId y availableSeats <= site.capacity
- UpdateEventService: validación de siteId y capacidad en actualizaciones
- DeleteEventService: limpieza de category_event al eliminar evento DRAFT
- UploadEventMediaService: verificación de existencia del evento antes de subir media
- UpdateEventRequest: validaciones Jakarta en todos los campos editables
- GetEventService: categoryId y siteName ahora se populan correctamente en todas las respuestas GET
- CreateEventRequest: límite superior de 100000 en availableSeats
- CreateEventService y UpdateEventService: siteName populado en EventResponse

### Added
- GET /api/v1/events/public: lista todos los eventos publicados y públicos
- GET /api/v1/events/public/city/{cityId}: filtra por ciudad
- GET /api/v1/events/public/category/{categoryId}: filtra por categoría
- GET /api/v1/events/public/date-range?from=&to=: filtra por rango de fechas
- EventSummaryResponse actualizado con siteName y categoryId

### Fixed
- UpdateEventService: permite actualizar la categoría de un evento via categoryId en el request
- UpdateEventService: categoryId ahora se popula correctamente en la respuesta del update
- AddCommentReplyRequest: parentReplyId con @Positive para rechazar valores <= 0
- Módulo category completo: tablas, dominio, infraestructura, aplicación e interfaces
- 14 endpoints REST para gestión de categorías y asignación a eventos
- Soporte de categorías jerárquicas mediante dad_id
- Seed inicial con 5 categorías base

## [develop] - 2026-05-13

### feat(storage): add image moderation via OpenNSFW2 + Sightengine cascade
#### Added
- `nsfw-service/`: FastAPI microservice wrapping OpenNSFW2 (`GET /health`, `POST /classify`). Returns `nsfw_score` (0.0–1.0) and `flagged` boolean. Rejects non-image content types and corrupted files with HTTP 400. Dockerized with `python:3.11-slim`.
- `ModerationPort` (port/out): secondary port with `isNsfwSafe(byte[])` and `isGoreSafe(byte[])`. Fail-open contract documented in Javadoc.
- `ImageModerationService` (application/service): implements `ModerationPort`, orchestrates the two-stage cascade — OpenNSFW2 first, Sightengine only if stage 1 passes. Throws `ImagePolicyViolationException` on rejection.
- `NsfwClientAdapter` (infrastructure/external): `RestTemplate` multipart POST to `${moderation.nsfw.url}/classify`. Fail-open on any infrastructure error.
- `SightengineAdapter` (infrastructure/external): `RestTemplate` multipart POST to `https://api.sightengine.com/1.0/check.json` with `models=gore`. Extracts `gore.prob`. Fail-open on any infrastructure error.
- `ImagePolicyViolationException` (shared/domain/exception): extends `BusinessException`. User-facing message is intentionally generic; internal reason code (`NSFW_CONTENT` / `VIOLENT_CONTENT`) is preserved for logging.
- `ModerationConfig` (infrastructure/config): `@ConfigurationProperties(prefix = "moderation")` with nested `Nsfw` and `Sightengine` classes. Declares `RestTemplate` bean.
- `nsfw-classifier` service in `docker-compose.yml`: internal-only (no host port), connected to `tuevento-network`, with healthcheck on `/health`. Backend `depends_on` updated.
- `moderation.*` properties added to `application-dev.yaml` (root-level, not under `app:`).
- Moderation environment variables added to `.env` and `.env.example`: `NSFW_SERVICE_URL`, `NSFW_THRESHOLD`, `SIGHTENGINE_API_USER`, `SIGHTENGINE_API_SECRET`, `SIGHTENGINE_MODELS`, `SIGHTENGINE_THRESHOLD`.

#### Modified
- `UploadFileUseCase`: injected `ImageModerationService` via `@RequiredArgsConstructor`. Moderation cascade called after `validateSize()` and before `storageClient.uploadFile()`. Skipped for non-image content types (`!contentType.startsWith("image/")`).
- `GlobalExceptionHandler`: added handler for `ImagePolicyViolationException` → HTTP 422 Unprocessable Entity.

#### Design decisions
- Cascade order (NSFW → gore) minimizes Sightengine API quota consumption (100 req/day free tier).
- Fail-open policy: moderation service failures never block legitimate uploads; errors are logged at ERROR level.
- Moderation is transparent to `StorageController` and `StorageClientPort` — zero changes to the REST interface or S3 adapter.

## [develop] - 2026-05-12

### feat(event): event module
#### Added
- Liquibase changesets 039–045: tablas `event`, `event_status_log`, `event_layout`, `event_media`, `event_media_log`, `event_rating`, `event_comment_reply` con FKs, constraints UNIQUE y CHECK via `sql` raw (compatible con Liquibase OSS)
- Domain layer: modelos puros (`Event`, `EventStatus`, `EventStatusLog`, `EventLayout`, `EventMedia`, `EventMediaLog`, `EventRating`, `EventCommentReply`), interfaces de repositorio sin dependencias de Spring, eventos de dominio inmutables (`EventCreatedEvent`, `EventStatusChangedEvent`, `EventCancelledEvent`, `EventRatingAddedEvent`, `EventMediaUploadedEvent`)
- Infrastructure layer: entidades JPA (`EventEntity` extiende `JpaAuditingEntity`, más 6 entidades sin auditoría), `JpaRepository` por entidad, mappers MapStruct, implementaciones `RepositoryImpl`; `EventMediaLogJpaRepository` incluye `@Query` para `findNextVersionByEventId`
- Application layer: 9 ports in (`CreateEventUseCase`, `UpdateEventUseCase`, `ChangeEventStatusUseCase`, `GetEventUseCase`, `DeleteEventUseCase`, `AddEventRatingUseCase`, `AddCommentReplyUseCase`, `UploadEventMediaUseCase`, `GetEventLayoutUseCase`, `SaveEventLayoutUseCase`), 9 use cases con validaciones de negocio (ownership, transiciones de estado `DRAFT→PUBLISHED→CANCELLED/COMPLETED`, unicidad, rating único por usuario, validación de layout antes de publicar)
- REST controllers: `EventController` (`/api/v1/events`), `EventRatingController` (`/api/v1/events/{eventId}/ratings`), `EventCommentController` (`/api/v1/ratings/{ratingId}/replies`), `EventMediaController` (`/api/v1/events/{eventId}/media`), `EventLayoutController` (`/api/v1/events/{eventId}/layout` — GET público + PUT `ORGANIZER`)
- `SecurityConfig` actualizado: 5 endpoints GET públicos en `PUBLIC_GET_ENDPOINTS`; `PUT /api/v1/events/*/layout` como autenticado; endpoints de escritura protegidos por rol via `@PreAuthorize`

#### Fixed
- Liquibase changeset 039: reemplazado `addCheckConstraint` (Liquibase Pro) por `sql` raw con `ALTER TABLE ... ADD CONSTRAINT ... CHECK (...)` — compatible con Liquibase OSS 4.x
- Campos `boolean isPublic` / `boolean isVisible` migrados a `Boolean` objeto en modelos de dominio, entidades JPA y DTOs de response para compatibilidad con MapStruct (Lombok genera `isXxx()` para `boolean` primitivo, que MapStruct resuelve como propiedad `xxx` en lugar de `isXxx`)
- `TestTuEventoApplication`: agregado import explícito de `TuEventoApplication` para resolver `cannot find symbol` durante `test-compile` con annotation processors activos
- `EventLayoutEntity`: agregado `@JdbcTypeCode(SqlTypes.JSON)` sobre `layoutData` para que Hibernate haga el binding correcto al tipo `jsonb` de PostgreSQL — sin esta anotación Hibernate enviaba el valor como `character varying` causando error de tipo en la columna
- `ChangeEventStatusService`: validación de layout antes de transición `DRAFT → PUBLISHED`; lanza `BusinessException("EVENT_LAYOUT_REQUIRED")` si no existe layout para el evento

## [develop] - 2026-04-24

### Theme module
#### Added
- Liquibase changeset 034: created `theme` table (id, name, description, default_palette jsonb)
- Liquibase changeset 035: created `user_theme` table with FK to `app_user` and `theme`; `is_active` flag controls the single active theme per user
- Liquibase changeset 036: created `theme_customization` table with FK to `user_theme`; stores per-property overrides as key/value pairs
- Liquibase changeset 037: created `theme_log` table with FK to `user_theme`; append-only audit log of UPDATE and RESET actions
- Liquibase changeset 038: seed data — 4 base themes with full `default_palette` JSON: `DARK` (deep purple, platform identity), `LIGHT`, `VIBRANT`, `ACCESSIBLE`
- Domain models: `Theme`, `UserTheme`, `ThemeCustomization`, `ThemeLog` — pure POJOs, no JPA, no Jackson
- Domain repository interfaces: `ThemeRepository`, `UserThemeRepository`, `ThemeCustomizationRepository`, `ThemeLogRepository` — pure interfaces, no Spring Data
- Domain events: `ThemeActivatedEvent`, `ThemeCustomizedEvent`, `ThemeCustomizationResetEvent` — immutable, primitive IDs only
- JPA entities: `ThemeEntity`, `UserThemeEntity`, `ThemeCustomizationEntity`, `ThemeLogEntity` — no `@ManyToOne`, FKs as plain `Integer`; no `JpaAuditingEntity` inheritance; `ThemeLogEntity` is append-only (no `@Setter`)
- JPA repositories: `ThemeJpaRepository`, `UserThemeJpaRepository` (`@Modifying @Query` for bulk deactivation), `ThemeCustomizationJpaRepository`, `ThemeLogJpaRepository`
- `ThemeInfraMapper`: single MapStruct mapper in `infrastructure/persistence/mapper/` handling all four entity↔domain conversions with explicit `@Mapping` for PK renames
- Domain repository implementations: `ThemeRepositoryImpl`, `UserThemeRepositoryImpl`, `ThemeCustomizationRepositoryImpl`, `ThemeLogRepositoryImpl`
- Application ports in: `GetThemesPort`, `ActivateThemePort`, `GetActivePalettePort`, `CustomizeThemePort`, `ResetCustomizationPort`, `GetThemeLogPort` — pure interfaces, no Spring
- DTOs response: `ThemeResponse` (id, name, description — no palette exposed), `ResolvedPaletteResponse` (themeId, themeName, userThemeId, `Map<String, Object>` palette), `ThemeLogResponse`
- DTO request: `CustomizeThemeRequest` (property `@NotBlank`, value `@NotBlank` + `@Pattern` for hex/rgb/rgba)
- `ThemeAppMapper`: MapStruct mapper for `Theme → ThemeResponse` and `ThemeLog → ThemeLogResponse`
- `ThemePaletteResolver`: `@Component` that parses `defaultPalette` JSON via `ObjectMapper` and overlays user customizations — decoupled from use cases
- Use cases: `GetThemesUseCase`, `ActivateThemeUseCase` (deactivates current, reactivates or creates `UserTheme`, publishes event), `GetActivePaletteUseCase` (auto-activates `DARK` theme if no active theme found), `CustomizeThemeUseCase` (upsert customization, audit log, event), `ResetCustomizationUseCase` (delete customization, audit log with `oldValue`, event), `GetThemeLogUseCase`
- `ThemeController`: base path `/api/v1/themes`, 6 endpoints (see below)
- `SecurityConfig` updated: `GET /api/v1/themes` added to public routes via `PUBLIC_GET_ENDPOINTS`; authenticated routes added as explicit `requestMatchers` by HTTP method

#### Endpoints
- `GET /api/v1/themes` — public; returns list of available themes
- `POST /api/v1/themes/activate/{themeId}` — authenticated; activates theme for current user, returns resolved palette
- `GET /api/v1/themes/my-active` — authenticated; returns current user's resolved palette (activates DARK by default if none set)
- `PUT /api/v1/themes/my-active/customize` — authenticated; overrides a palette property, returns updated resolved palette
- `DELETE /api/v1/themes/my-active/customize/{property}` — authenticated; resets a property to theme default, returns updated resolved palette
- `GET /api/v1/themes/my-active/log` — authenticated; returns audit log of theme changes for current user

#### Design decisions
- Resolved palette strategy: backend merges `default_palette` + user customizations and returns a ready-to-consume `Map<String, Object>` — both web (React + Tailwind v4) and mobile (React Native + NativeWind) share the same single source of truth
- `DARK` is the default theme (deep purple identity palette matching the existing frontend); auto-activated on first `GET /my-active` if the user has no active theme
- `ThemePaletteResolver` kept as a separate `@Component` to avoid coupling merge logic to individual use cases

## [develop] - 2026-04-07

### Security module
#### Fixed
- Fixed: activation code not validated against request email (security vulnerability)
- Fixed: recovery code not validated against request email (security vulnerability)
- Fixed: logout not validating session ownership (security vulnerability)
- Fixed: login not checking BLOCKED/INACTIVE/DELETED user status
- Fixed: lockout message exposed exact timestamp
- Fixed: recover password revealed email existence (user enumeration)
- Reverted: recover password now returns explicit error when email is not registered
- Fixed: refresh token not validating user active status
- Fixed: email send failure in register now logged instead of propagated
- Fixed: OAuth login allows duplicate email with local account
- Fixed: organizer request allowed for existing organizers
#### Added
- Added: AccessDeniedException handler (403)
- Added: input validations on register: Gmail-only email, strong password, full name format
- Added: password strength validation on change password to match register rules- Added: AuthenticationException handler (401)
- Added: MethodNotAllowedException handler (405)
- Added: ConstraintViolationException handler (409)
- Added: NoResourceFoundException handler (404)

### Profile module
#### Added
- Liquibase changeset 031: inserted `PROFILE_PICTURE` file category (public, jpg/jpeg/png/webp, max 2MB)
- Liquibase changeset 032: created `profile` table with FK to `app_user`, `city` (nullable) and `stored_file` (nullable)
- Liquibase changeset 033: created `profile_log` table with FK to `profile`
- Domain models: `Profile` (references `City` object from geolocation module), `ProfileLog`
- Domain repository interfaces: `ProfileRepository`, `ProfileLogRepository`
- Domain events: `ProfileCreatedEvent`, `ProfileUpdatedEvent` (primitive IDs only)
- Domain exception: `ProfileAlreadyExistsException`
- JPA entities: `ProfileEntity` (auditable, `storedFileId` as plain INT decoupled from storage module), `ProfileLogEntity`
- MapStruct mappers: `ProfileMapper` (uses `CityMapper`), `ProfileLogMapper`
- JPA repositories: `ProfileJpaRepository`, `ProfileLogJpaRepository`
- Domain repository implementations: `ProfileRepositoryImpl`, `ProfileLogRepositoryImpl`
- Application ports in: `CreateProfilePort`, `UpdateProfilePort`, `GetProfilePort`, `GetProfileByUserIdPort`
- DTOs: `CreateProfileRequest` (userId, fullName), `UpdateProfileRequest` (cityId, storedFileId, fullName, bio), `ProfileResponse`
- Use cases: `CreateProfileUseCase` (validates no duplicate profile per user, assigns default avatar), `UpdateProfileUseCase` (logs each changed field to profile_log), `GetProfileUseCase`, `GetProfileByUserIdUseCase`
- `ProfileController`: POST `/api/v1/profiles`, PUT `/api/v1/profiles/{profileId}`, GET `/api/v1/profiles/{profileId}`, GET `/api/v1/profiles/user/{userId}`
- `ProfileDataInitializer`: uploads `default-avatar.jpg` from `src/main/resources/assets/` to MinIO on first startup using `UploadFilePort`; logs resulting `storedFileId` for manual config
- `app.profile.default-avatar-stored-file-id` property added to `application-dev.yaml`
- GET profile endpoints added to public routes in `SecurityConfig`
- `src/main/resources/assets/default-avatar.jpg` placeholder directory created

#### Known issues
- `ProfileDataInitializer` lookup for existing default avatar pending fix (method to check `findByOwnerEntity` not yet validated)

## [develop] - 2026-03-29

### Storage module
#### Added
- MinIO service added to docker-compose for local S3-compatible storage
- AWS S3 SDK dependency added to pom.xml
- Liquibase changesets 018-022: storage_provider, file_category, stored_file, storage_operation_log tables and initial MinIO provider
- S3StorageClient: AWS S3 SDK implementation of StorageClientPort compatible with MinIO
- S3Config: S3Client and S3Presigner beans configured for MinIO in development
- StorageController: REST endpoints for file upload, delete, get and url generation
- Initial file category: ORGANIZER_DOCUMENT (PDF, max 5MB, private)
- organizer_petition migrated from BYTEA document to stored_file_id FK referencing storage module
- CreateProfileRequest: added storedFileIdSet flag to distinguish explicit null from use-default behavior

### Geolocation module#### Added
- Liquibase changesets 025-027: department, city and site tables
- GeolocationController: REST endpoints for departments, cities and sites
- Seed data: 33 departments and 1122 cities of Colombia via Liquibase loadData (changesets 028-029)

### Backend

### Security module — completed and merged from feature/security-module
#### Added
- Liquibase changesets for all security tables: user_status, role, permission, app_user, role_permission, login_credentials, account_activation, account_lockout, auth_session, refresh_token, recover_password, password_history, oauth_account, user_status_history, organizer_petition
- Audit columns (created_at, updated_at, created_by, updated_by) to all auditable tables
- Domain models: User, Role, Permission, UserStatus, UserStatusHistory, AuthSession, RefreshToken, LoginCredentials, AccountActivation, AccountLockout, RecoverPassword, PasswordHistory, OauthAccount, OrganizerPetition
- Domain repository interfaces for all models
- Domain events: UserRegisteredEvent, UserActivatedEvent, UserLockedEvent, PasswordChangedEvent, OrganizerPetitionCreatedEvent
- JPA entities and MapStruct mappers for all models
- JPA repository implementations
- Domain repository implementations
- Application ports (in/out) for all use cases
- Request/Response DTOs with Jakarta validation
- Use cases: RegisterUser, ActivateAccount, Login, Logout, RefreshToken, RecoverPassword, ResetPassword, ChangePassword, LinkOauthAccount, OauthLogin, RequestOrganizer
- AliasGenerator utility for automatic alias generation from email
- OauthProfile DTO for OAuth provider profile mapping
- External port implementations: JwtTokenGenerator (JJWT), BcryptPasswordEncoder, JavaMailEmailNotification, SecureRandomCodeGenerator
- OAuth Google configuration with extensible provider pattern
- Spring Security configuration with JWT authentication filter
- REST controllers: AuthController, UserController
- Initial data: roles (ADMIN, ORGANIZER, USER) and user statuses (PENDING, ACTIVE, BLOCKED, INACTIVE, DELETED) via Liquibase changeset
- DataInitializer: default admin user created on startup from environment variables
- SecurityDataCleanupTask: scheduled task running daily at 2am to clean expired/revoked auth sessions, refresh tokens, activation codes and recovery codes
- Swagger JWT Bearer Authentication scheme configured for protected endpoints
- Admin endpoints for organizer petition management: list pending requests, approve and reject
- OrganizerApprovedEvent domain event

#### Fixed
- Duplicate SecurityConfig bean conflict resolved
- Missing audit columns added to auditable tables via changeset 016
- BYTEA mapping fixed in OrganizerPetitionEntity replacing @Lob with @Column(columnDefinition = "bytea")
- RegisterUserUseCase: default role code corrected from "ATTENDEE" to "USER"
- OauthLoginUseCase: default role code corrected from "ATTENDEE" to "USER"
- ActivateAccountUseCase: user status now updated to "ACTIVE" after successful account activation
- ChangePasswordUseCase: fixed user lookup using SecurityUser.getUserId() instead of Authentication.getName() which was returning alias instead of email
- SecurityUser: fixed authority registration removing ROLE_ prefix to match hasAuthority("ADMIN") checks in SecurityConfig and AdminController
- JwtAuthenticationFilter: replaced fragile manual Base64 claim parser with JJWT-based extraction via TokenGeneratorPort; added debug log for loaded authorities
- SecurityConfig: added @EnableMethodSecurity to enable @PreAuthorize on controllers
- TokenGeneratorPort / JwtTokenGenerator: added extractRole() and extractSubject() methods
