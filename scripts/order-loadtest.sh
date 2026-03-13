#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://127.0.0.1:8080}"
REQUESTS="${2:-300}"
CONCURRENCY="${3:-30}"
PASSWORD="Password123"
RUN_ID="$(date +%s)"
RESULT_FILE="$(mktemp)"

echo "[order-loadtest] base_url=${BASE_URL} requests=${REQUESTS} concurrency=${CONCURRENCY}"

create_body=$(cat <<JSON
{"name":"loadtest-product-${RUN_ID}","description":"order flow load test","price":9.90,"stock":$((REQUESTS * 5))}
JSON
)
create_resp="$(curl -fsS -X POST "${BASE_URL}/api/v1/products" \
  -H "Content-Type: application/json" \
  -d "${create_body}")"

product_id="$(printf '%s' "${create_resp}" | sed -n 's/.*"id":\([0-9][0-9]*\).*/\1/p')"
if [[ -z "${product_id}" ]]; then
  echo "[order-loadtest] failed to create load-test product"
  rm -f "${RESULT_FILE}"
  exit 1
fi

curl -fsS -X PUT "${BASE_URL}/api/v1/products/${product_id}/on-shelf" >/dev/null

start_ts="$(date +%s)"

seq "${REQUESTS}" | xargs -I{} -P "${CONCURRENCY}" bash -c '
  idx="$1"
  base_url="$2"
  password="$3"
  run_id="$4"
  product_id="$5"

  email="order-load-${run_id}-${idx}@example.com"
  register_body="{\"email\":\"${email}\",\"password\":\"${password}\",\"nickname\":\"order-load-${idx}\"}"
  login_body="{\"email\":\"${email}\",\"password\":\"${password}\"}"
  add_cart_body="{\"productId\":${product_id},\"quantity\":1}"

  register_resp="$(curl -fsS -X POST "${base_url}/api/v1/users/register" \
    -H "Content-Type: application/json" \
    -d "${register_body}" 2>/dev/null || true)"
  if [[ -z "${register_resp}" ]]; then
    echo FAIL
    exit 0
  fi

  login_resp="$(curl -fsS -X POST "${base_url}/api/v1/users/login" \
    -H "Content-Type: application/json" \
    -d "${login_body}" 2>/dev/null || true)"
  token="$(printf "%s" "${login_resp}" | sed -n "s/.*\"token\":\"\\([^\"]*\\)\".*/\\1/p")"
  if [[ -z "${token}" ]]; then
    echo FAIL
    exit 0
  fi

  add_ok="$(curl -fsS -o /dev/null -w "%{http_code}" -X POST "${base_url}/api/v1/cart/items" \
    -H "Authorization: Bearer ${token}" \
    -H "Content-Type: application/json" \
    -d "${add_cart_body}" 2>/dev/null || true)"
  if [[ "${add_ok}" != "200" ]]; then
    echo FAIL
    exit 0
  fi

  order_resp="$(curl -fsS -X POST "${base_url}/api/v1/orders" \
    -H "Authorization: Bearer ${token}" 2>/dev/null || true)"
  order_id="$(printf "%s" "${order_resp}" | sed -n "s/.*\"id\":\\([0-9][0-9]*\\).*/\\1/p")"
  amount="$(printf "%s" "${order_resp}" | sed -n "s/.*\"totalAmount\":\\([0-9.]*\\).*/\\1/p")"
  if [[ -z "${order_id}" || -z "${amount}" ]]; then
    echo FAIL
    exit 0
  fi

  pay_body="{\"orderId\":${order_id},\"transactionId\":\"order-load-tx-${run_id}-${idx}\",\"paidAmount\":${amount}}"
  pay_ok="$(curl -fsS -o /dev/null -w "%{http_code}" -X POST "${base_url}/api/v1/payments/mock-callback" \
    -H "Content-Type: application/json" \
    -d "${pay_body}" 2>/dev/null || true)"
  if [[ "${pay_ok}" != "200" ]]; then
    echo FAIL
    exit 0
  fi

  complete_ok="$(curl -fsS -o /dev/null -w "%{http_code}" -X POST "${base_url}/api/v1/orders/${order_id}/complete" \
    -H "Authorization: Bearer ${token}" 2>/dev/null || true)"
  if [[ "${complete_ok}" != "200" ]]; then
    echo FAIL
    exit 0
  fi

  echo OK
' _ {} "${BASE_URL}" "${PASSWORD}" "${RUN_ID}" "${product_id}" > "${RESULT_FILE}" || true

end_ts="$(date +%s)"
duration=$((end_ts - start_ts))
if [[ "${duration}" -lt 1 ]]; then
  duration=1
fi

ok_count="$(grep -c '^OK$' "${RESULT_FILE}" || true)"
fail_count=$((REQUESTS - ok_count))
rps=$((REQUESTS / duration))
success_rate="$(awk -v ok="${ok_count}" -v total="${REQUESTS}" 'BEGIN { printf "%.2f", (ok * 100.0 / total) }')"

echo "[order-loadtest] product_id=${product_id}"
echo "[order-loadtest] duration_seconds=${duration}"
echo "[order-loadtest] success=${ok_count} fail=${fail_count} success_rate=${success_rate}%"
echo "[order-loadtest] throughput_rps=${rps}"

rm -f "${RESULT_FILE}"
