#!/usr/bin/env bash
# Arranca el backend Spring Boot cargando las variables desde .env
# Uso: ./run-backend.sh  (desde cualquier directorio)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

set -a
# shellcheck source=.env
source "$SCRIPT_DIR/.env"
set +a

exec "$SCRIPT_DIR/TuEventoBackend/tu-evento/mvnw" \
  -f "$SCRIPT_DIR/TuEventoBackend/tu-evento/pom.xml" \
  spring-boot:run
