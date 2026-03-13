#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://127.0.0.1:8080}"
EMAIL="smoke-$(date +%s)@example.com"
PASSWORD="Password123"

echo "[smoke] base_url=${BASE_URL}"

curl -fsS "${BASE_URL}/api/v1/health" >/dev/null
curl -fsS "${BASE_URL}/actuator/health" >/dev/null
curl -fsS "${BASE_URL}/actuator/metrics" >/dev/null
curl -fsS "${BASE_URL}/api/v1/products" >/dev/null

register_body=$(cat <<JSON
{"email":"${EMAIL}","password":"${PASSWORD}","nickname":"smoke-user"}
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
  echo "[smoke] failed to parse token"
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
  echo "[smoke] failed to parse order response"
  exit 1
fi

pay_body=$(cat <<JSON
{"orderId":${order_id},"transactionId":"smoke-tx-${order_id}","paidAmount":${amount}}
JSON
)
curl -fsS -X POST "${BASE_URL}/api/v1/payments/mock-callback" \
  -H "Content-Type: application/json" \
  -d "${pay_body}" >/dev/null

curl -fsS -X POST "${BASE_URL}/api/v1/orders/${order_id}/complete" \
  -H "Authorization: Bearer ${token}" >/dev/null

echo "[smoke] success"
