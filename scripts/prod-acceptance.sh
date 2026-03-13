#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://127.0.0.1:8080}"
LIGHT_REQUESTS="${2:-500}"
LIGHT_CONCURRENCY="${3:-20}"
ORDER_REQUESTS="${4:-300}"
ORDER_CONCURRENCY="${5:-30}"

assert_contains() {
  local content="$1"
  local expected="$2"
  local message="$3"
  if [[ "${content}" != *"${expected}"* ]]; then
    echo "[prod-acceptance] ${message}"
    exit 1
  fi
}

assert_line_contains() {
  local content="$1"
  local pattern="$2"
  local expected="$3"
  local message="$4"
  local line
  line="$(printf '%s\n' "${content}" | grep -F "${pattern}" || true)"
  if [[ -z "${line}" || "${line}" != *"${expected}"* ]]; then
    echo "[prod-acceptance] ${message}"
    exit 1
  fi
}

echo "[prod-acceptance] base_url=${BASE_URL}"
echo "[prod-acceptance] light_load=${LIGHT_REQUESTS}/${LIGHT_CONCURRENCY} order_load=${ORDER_REQUESTS}/${ORDER_CONCURRENCY}"

compose_ps="$(docker compose ps)"
assert_contains "${compose_ps}" "ecommerce-app" "app container missing from docker compose ps"
assert_contains "${compose_ps}" "ecommerce-mysql" "mysql container missing from docker compose ps"
assert_contains "${compose_ps}" "ecommerce-redis" "redis container missing from docker compose ps"
assert_contains "${compose_ps}" "ecommerce-rabbitmq" "rabbitmq container missing from docker compose ps"
assert_line_contains "${compose_ps}" "ecommerce-app" "Up" "app container is not running"
assert_line_contains "${compose_ps}" "ecommerce-mysql" "(healthy)" "mysql container is not healthy"
assert_line_contains "${compose_ps}" "ecommerce-redis" "(healthy)" "redis container is not healthy"
assert_line_contains "${compose_ps}" "ecommerce-rabbitmq" "(healthy)" "rabbitmq container is not healthy"

api_health="$(curl -fsS "${BASE_URL}/api/v1/health")"
assert_contains "${api_health}" "\"code\":\"OK\"" "api health did not return code=OK"
assert_contains "${api_health}" "\"status\":\"UP\"" "api health did not return status=UP"

actuator_health="$(curl -fsS "${BASE_URL}/actuator/health")"
assert_contains "${actuator_health}" "\"status\":\"UP\"" "actuator health did not return status=UP"
assert_contains "${actuator_health}" "\"db\":{\"status\":\"UP\"" "db component is not UP"
assert_contains "${actuator_health}" "\"redis\":{\"status\":\"UP\"" "redis component is not UP"
assert_contains "${actuator_health}" "\"rabbit\":{\"status\":\"UP\"" "rabbit component is not UP"

root_body="$(curl -fsS "${BASE_URL}/")"
assert_contains "${root_body}" "\"service\":\"ecommerce-practice\"" "root endpoint response is unexpected"

not_found_file="$(mktemp)"
not_found_code="$(curl -sS -o "${not_found_file}" -w "%{http_code}" "${BASE_URL}/this-path-should-not-exist")"
not_found_body="$(cat "${not_found_file}")"
rm -f "${not_found_file}"
if [[ "${not_found_code}" != "404" ]]; then
  echo "[prod-acceptance] expected 404 for unknown path, got ${not_found_code}"
  exit 1
fi
assert_contains "${not_found_body}" "\"code\":\"NOT_FOUND\"" "unknown path did not return code=NOT_FOUND"

./scripts/smoke.sh "${BASE_URL}"
./scripts/mq-smoke.sh "${BASE_URL}"

light_output="$(./scripts/loadtest.sh "${BASE_URL}" "${LIGHT_REQUESTS}" "${LIGHT_CONCURRENCY}")"
printf '%s\n' "${light_output}"
assert_contains "${light_output}" "fail=0" "lightweight load test reported failures"

order_output="$(./scripts/order-loadtest.sh "${BASE_URL}" "${ORDER_REQUESTS}" "${ORDER_CONCURRENCY}")"
printf '%s\n' "${order_output}"
assert_contains "${order_output}" "fail=0" "order-flow load test reported failures"

echo "[prod-acceptance] success"
