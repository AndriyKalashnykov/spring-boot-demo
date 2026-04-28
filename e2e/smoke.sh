#!/usr/bin/env bash
# E2E smoke test: boots the packaged JAR on a free port, exercises CRUD,
# health, swagger, and commit-info endpoints, then tears down. Uses an
# ephemeral port (kernel-allocated via Spring's `--server.port=0`) and
# reads the actual port back from the application log so concurrent
# Surefire/Failsafe runs and parallel CI jobs don't collide.

set -euo pipefail

JAR="${JAR:-target/spring-boot-demo-0.0.1.jar}"
LOG="${LOG:-target/e2e-smoke.log}"
APP_PID=""

cleanup() {
  if [[ -n "${APP_PID}" ]] && kill -0 "${APP_PID}" 2>/dev/null; then
    echo "Stopping app (pid=${APP_PID})..."
    kill "${APP_PID}" 2>/dev/null || true
    wait "${APP_PID}" 2>/dev/null || true
  fi
}
trap cleanup EXIT

if [[ ! -f "${JAR}" ]]; then
  echo "ERROR: JAR not found at ${JAR}. Run 'make build' first." >&2
  exit 1
fi

# Pre-allocate a free port from the kernel's ephemeral range so concurrent
# CI runs / local sessions don't collide. application.yml shares the
# management server with `server.port`, so a single port covers everything.
PORT=$(python3 -c 'import socket;s=socket.socket();s.bind(("",0));print(s.getsockname()[1]);s.close()')

mkdir -p "$(dirname "${LOG}")"
: > "${LOG}"

echo "=== E2E: starting ${JAR} on port ${PORT} ==="
java -jar "${JAR}" \
  --server.port="${PORT}" \
  --spring.profiles.active=default \
  >"${LOG}" 2>&1 &
APP_PID=$!

# Wait up to 90s for boot. Match either "Started Application in N seconds"
# or "Started HotelApplication in N seconds".
for _ in $(seq 1 90); do
  if grep -qE 'Started [A-Za-z]+ in [0-9]+\.[0-9]+ seconds' "${LOG}"; then
    break
  fi
  if ! kill -0 "${APP_PID}" 2>/dev/null; then
    echo "FAIL: app exited during startup. Last 30 log lines:" >&2
    tail -30 "${LOG}" >&2
    exit 1
  fi
  sleep 1
done

if ! grep -qE 'Started [A-Za-z]+ in [0-9]+\.[0-9]+ seconds' "${LOG}"; then
  echo "FAIL: app did not finish booting within 90s. Last 30 log lines:" >&2
  tail -30 "${LOG}" >&2
  exit 1
fi

BASE="http://127.0.0.1:${PORT}"
echo "=== E2E: app booted on ${BASE} ==="

PASS=0
FAIL=0

assert_status() {
  local method="$1" url="$2" expected="$3" body="${4:-}"
  local opts=(-s -o /dev/null -w '%{http_code}' -X "${method}")
  [[ -n "${body}" ]] && opts+=(-H 'Content-Type: application/json' -d "${body}")
  local status
  status=$(curl "${opts[@]}" "${url}")
  if [[ "${status}" == "${expected}" ]]; then
    echo "PASS: ${method} ${url} -> ${status}"
    PASS=$((PASS + 1))
  else
    echo "FAIL: ${method} ${url} -> ${status} (expected ${expected})"
    FAIL=$((FAIL + 1))
  fi
}

assert_body_contains() {
  local url="$1" expected="$2"
  local body
  body=$(curl -sf "${url}" || true)
  if echo "${body}" | grep -q "${expected}"; then
    echo "PASS: GET ${url} contains '${expected}'"
    PASS=$((PASS + 1))
  else
    echo "FAIL: GET ${url} missing '${expected}' (body: ${body:0:200})"
    FAIL=$((FAIL + 1))
  fi
}

# Health + readiness
assert_status GET  "${BASE}/actuator/health"             200
assert_body_contains "${BASE}/actuator/health"           '"status":"UP"'

# OpenAPI / Swagger surface
assert_status GET  "${BASE}/v3/api-docs"                 200
assert_status GET  "${BASE}/swagger-ui/index.html"       200

# CRUD round-trip
HOTEL_PAYLOAD='{"name":"E2E Hotel","description":"smoke test","city":"Tucson","rating":3}'
LOCATION=$(curl -sfi -X POST "${BASE}/example/v1/hotels" \
  -H 'Content-Type: application/json' \
  -d "${HOTEL_PAYLOAD}" | tr -d '\r' | grep -i '^Location:' | awk '{print $2}' || true)

if [[ -n "${LOCATION}" ]]; then
  echo "PASS: POST /example/v1/hotels -> 201 Location=${LOCATION}"
  PASS=$((PASS + 1))
  ID="${LOCATION##*/}"
  assert_status        GET    "${BASE}/example/v1/hotels/${ID}" 200
  assert_body_contains "${BASE}/example/v1/hotels/${ID}"        'E2E Hotel'
  assert_status        DELETE "${BASE}/example/v1/hotels/${ID}" 204
  assert_status        GET    "${BASE}/example/v1/hotels/${ID}" 404
else
  echo "FAIL: POST /example/v1/hotels did not return a Location header"
  FAIL=$((FAIL + 1))
fi

# Pagination
assert_status GET "${BASE}/example/v1/hotels?page=0&size=10" 200

# Negative case (numeric path bound to Long; use a high ID that won't exist)
assert_status GET "${BASE}/example/v1/hotels/9999999" 404

# Build/commit metadata endpoint
assert_status GET "${BASE}/commitid" 200

echo ""
echo "=== E2E results: ${PASS} passed, ${FAIL} failed ==="
[[ "${FAIL}" -eq 0 ]]
