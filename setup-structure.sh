#!/usr/bin/env bash
# setup-structure.sh
# Genera la estructura DDD modular para Tu Evento (CapySoft)
# Ejecutar desde la raíz del monorepo

BASE="TuEventoBackend/tu-evento/src/main/java/com/capysoft/tuevento"

# ─── Módulos y sus capas ────────────────────────────────────────────────────

# Todos los módulos
MODULES=(security profile event ticket payment wallet notification seat section category theme language geolocation)

# Reglas especiales
WEBSOCKET_MODULES=(notification ticket)
MESSAGING_MODULES=(payment wallet notification ticket event)
EXTERNAL_MODULES=(payment notification geolocation)

contains() {
  local target="$1"; shift
  for item in "$@"; do [[ "$item" == "$target" ]] && return 0; done
  return 1
}

for MODULE in "${MODULES[@]}"; do
  M="$BASE/modules/$MODULE"

  # domain
  mkdir -p "$M/domain/model"
  mkdir -p "$M/domain/repository"
  mkdir -p "$M/domain/event"

  # application
  mkdir -p "$M/application/usecase"
  mkdir -p "$M/application/dto/request"
  mkdir -p "$M/application/dto/response"
  mkdir -p "$M/application/port/in"
  mkdir -p "$M/application/port/out"
  mkdir -p "$M/application/mapper"

  # infrastructure/persistence
  mkdir -p "$M/infrastructure/persistence/entity"
  mkdir -p "$M/infrastructure/persistence/repository"

  # infrastructure/messaging (solo para módulos seleccionados)
  if contains "$MODULE" "${MESSAGING_MODULES[@]}"; then
    mkdir -p "$M/infrastructure/messaging"
  fi

  # infrastructure/external (solo para módulos seleccionados)
  if contains "$MODULE" "${EXTERNAL_MODULES[@]}"; then
    mkdir -p "$M/infrastructure/external"
  fi

  # interfaces/rest
  mkdir -p "$M/interfaces/rest"

  # interfaces/websocket (solo para módulos seleccionados)
  if contains "$MODULE" "${WEBSOCKET_MODULES[@]}"; then
    mkdir -p "$M/interfaces/websocket"
  fi
done

# ─── Shared ─────────────────────────────────────────────────────────────────

SHARED="$BASE/shared"

mkdir -p "$SHARED/domain/exception"
mkdir -p "$SHARED/domain/valueobject"
mkdir -p "$SHARED/infrastructure/config"
mkdir -p "$SHARED/infrastructure/security"
mkdir -p "$SHARED/interfaces/advice"

# ─── .gitkeep en cada carpeta hoja ──────────────────────────────────────────

find "$BASE" -type d -empty -exec touch {}/.gitkeep \;

echo "✅  Estructura DDD generada en: $BASE"
