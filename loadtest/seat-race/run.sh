#!/usr/bin/env bash
# Prueba de carrera sobre la reserva de sillas (POST /api/v1/seats/{id}/reserve).
# Requiere: backend corriendo (./run-backend.sh) y el contenedor tuevento-postgres arriba.
# Uso: ./loadtest/seat-race/run.sh <seatId> [usuarios=50]
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$DIR/../.." && pwd)"
SEAT_ID="${1:?Uso: run.sh <seatId> [usuarios]}"
USUARIOS="${2:-50}"
BASE_URL="${BASE_URL:-http://host.docker.internal:8080}"
K6_IMAGE="grafana/k6:1.0.0"

psql_db() {
  docker exec -i tuevento-postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 -qAt "$@"' sh "$@"
}

estado=$(psql_db -c "SELECT status FROM seat WHERE seat_id = $SEAT_ID")
if [ "$estado" != "AVAILABLE" ]; then
  echo "La silla $SEAT_ID no está AVAILABLE (estado: '${estado:-no existe}'). Elige otra." >&2
  exit 1
fi

# Al terminar, la silla vuelve a quedar disponible como estaba
restaurar() {
  psql_db -c "UPDATE seat SET status = 'AVAILABLE', reserved_by = NULL, reserved_until = NULL WHERE seat_id = $SEAT_ID" >/dev/null
  echo "Silla $SEAT_ID restaurada a AVAILABLE."
}
trap restaurar EXIT

USER_IDS=$(psql_db -v usuarios="$USUARIOS" < "$DIR/setup.sql" | tail -n 1)
echo "Usuarios de prueba: $(echo "$USER_IDS" | tr ',' '\n' | wc -l | tr -d ' ')"

# Solo se toma JWT_SECRET del .env y se pasa a k6 sin que aparezca en la línea de comandos
if [ -z "${JWT_SECRET:-}" ]; then
  JWT_SECRET=$(grep -E '^JWT_SECRET=' "$ROOT/.env" | tail -n 1 | cut -d= -f2- | tr -d '\r' | sed -E "s/^[\"'](.*)[\"']$/\1/")
fi
export JWT_SECRET SEAT_ID USER_IDS BASE_URL

set +e
docker run --rm -i -e JWT_SECRET -e SEAT_ID -e USER_IDS -e BASE_URL "$K6_IMAGE" run --quiet - < "$DIR/seat-race.js"
k6_exit=$?
set -e

echo "Estado final de la silla:"
psql_db -c "SELECT seat_id, status, reserved_by, reserved_until FROM seat WHERE seat_id = $SEAT_ID"
exit $k6_exit
