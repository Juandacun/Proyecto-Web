#!/usr/bin/env bash
# Ejecuta las pruebas de sistema (E2E, Playwright) y el QA responsivo.
#
#   ./e2e/run-e2e.sh                     # todas las pruebas E2E
#   ./e2e/run-e2e.sh FlujoCriticoE2ETest # una clase concreta
#   E2E_HEADED=1 E2E_SLOWMO=800 ./e2e/run-e2e.sh   # ver el navegador paso a paso
#
# Levanta el backend con una base H2 EN MEMORIA y limpia (datos de data.sql) y la SPA
# (ng serve). Si ya hay algo escuchando en 8080 / 4200 lo reutiliza y no lo detiene;
# en ese caso la BD puede no estar limpia y el flujo crítico no será repetible.
#
# Variables: FRONT_DIR (carpeta de la SPA, por defecto cliente-frontend/),
#            NODE_BIN (carpeta bin de un Node >= 22.22 si el del sistema es antiguo).
set -euo pipefail

RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
FRONT_DIR="${FRONT_DIR:-$RAIZ/cliente-frontend}"
LOGS="$RAIZ/target/e2e-logs"
mkdir -p "$LOGS"
[ -n "${NODE_BIN:-}" ] && export PATH="$NODE_BIN:$PATH"

PIDS=()
limpiar() { for p in "${PIDS[@]:-}"; do [ -n "$p" ] && kill "$p" 2>/dev/null || true; done; }
trap limpiar EXIT

esperar() { # url timeout_s nombre
  local fin=$((SECONDS + $2))
  until curl -s -o /dev/null "$1"; do
    [ $SECONDS -ge $fin ] && { echo "Tiempo agotado esperando $3 ($1). Ver $LOGS" >&2; exit 1; }
    sleep 2
  done
  echo "✔ $3 listo"
}

if curl -s -o /dev/null http://localhost:8080/api/recursos; then
  echo "• Backend ya activo en :8080 (se reutiliza)"
else
  echo "• Compilando y levantando backend (H2 en memoria)…"
  (cd "$RAIZ" && ./mvnw -q -DskipTests package)
  java -jar "$RAIZ"/target/proyectoweb-*-SNAPSHOT.jar \
    --spring.datasource.url="jdbc:h2:mem:e2e;DB_CLOSE_DELAY=-1" > "$LOGS/backend.log" 2>&1 &
  PIDS+=($!)
  esperar http://localhost:8080/api/recursos 300 "Backend"
fi

if curl -s -o /dev/null http://localhost:4200; then
  echo "• SPA ya activa en :4200 (se reutiliza)"
else
  echo "• Levantando SPA (ng serve)…"
  [ -d "$FRONT_DIR/node_modules" ] || (cd "$FRONT_DIR" && npm ci)
  (cd "$FRONT_DIR" && npx ng serve --port 4200 > "$LOGS/frontend.log" 2>&1) &
  PIDS+=($!)
  esperar http://localhost:4200 300 "SPA"
fi

ARGS=(-Pe2e test "-De2e.headless=$([ -n "${E2E_HEADED:-}" ] && echo false || echo true)" "-De2e.slowMo=${E2E_SLOWMO:-0}")
[ $# -gt 0 ] && ARGS+=("-Dtest=$1")
cd "$RAIZ" && ./mvnw "${ARGS[@]}"
echo "Capturas en target/e2e-screenshots · informes en target/surefire-reports"
