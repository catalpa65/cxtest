#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://127.0.0.1:8080}"
EMAIL="mq-smoke-$(date +%s)@example.com"
PASSWORD="Password123"

wait_for_app() {
  local url="$1"
  local retries="${2:-60}"
  local delay_seconds="${3:-1}"
  local i
  for i in $(seq 1 "${retries}"); do
    if curl -fsS "${url}/actuator/health" >/dev/null 2>&1; then
      return 0
    fi
    sleep "${delay_seconds}"
  done

  echo "[mq-smoke] app did not become ready in time: ${url}"
  exit 1
}

echo "[mq-smoke] base_url=${BASE_URL}"

wait_for_app "${BASE_URL}"

register_body=$(cat <<JSON
{"email":"${EMAIL}","password":"${PASSWORD}","nickname":"mq-smoke-user"}
JSON
)
curl -fsS -X POST "${BASE_URL}/api/v1/users/register" \
  -H "Content-Type: application/json" \
  -d "${register_body}" >/dev/null

login_body=$(cat <<JSON
{"email":"${EMAIL}","password":"${PASSWORD}"}
JSON
)
login_resp="$(curl -fsS -X POST "${BASE_URL}/api/v1/users/login" \
  -H "Content-Type: application/json" \
  -d "${login_body}")"
token="$(printf '%s' "${login_resp}" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')"
if [[ -z "${token}" ]]; then
  echo "[mq-smoke] failed to parse token"
  exit 1
fi

add_cart_body='{"productId":1,"quantity":1}'
curl -fsS -X POST "${BASE_URL}/api/v1/cart/items" \
  -H "Authorization: Bearer ${token}" \
  -H "Content-Type: application/json" \
  -d "${add_cart_body}" >/dev/null

order_resp="$(curl -fsS -X POST "${BASE_URL}/api/v1/orders" \
  -H "Authorization: Bearer ${token}")"
order_id="$(printf '%s' "${order_resp}" | sed -n 's/.*"id":\([0-9][0-9]*\).*/\1/p')"
amount="$(printf '%s' "${order_resp}" | sed -n 's/.*"totalAmount":\([0-9.]*\).*/\1/p')"
if [[ -z "${order_id}" || -z "${amount}" ]]; then
  echo "[mq-smoke] failed to parse order response"
  exit 1
fi

pay_body=$(cat <<JSON
{"orderId":${order_id},"transactionId":"mq-smoke-tx-${order_id}","paidAmount":${amount}}
JSON
)
curl -fsS -X POST "${BASE_URL}/api/v1/payments/mock-callback" \
  -H "Content-Type: application/json" \
  -d "${pay_body}" >/dev/null

matched=0
for i in $(seq 1 20); do
  logs_resp="$(curl -fsS "${BASE_URL}/api/v1/mq/process-logs?limit=200")"
  if printf '%s' "${logs_resp}" | grep -q '"type":"ORDER_PAID_NOTIFY"' \
      && printf '%s' "${logs_resp}" | grep -q '"status":"SUCCESS"'; then
    matched=1
    break
  fi
  sleep 0.2
done

if [[ "${matched}" -ne 1 ]]; then
  echo "[mq-smoke] failed to observe ORDER_PAID_NOTIFY success log"
  exit 1
fi

curl -fsS "${BASE_URL}/api/v1/mq/dead-letters" >/dev/null
echo "[mq-smoke] success"
