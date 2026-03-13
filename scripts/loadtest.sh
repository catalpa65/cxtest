#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://127.0.0.1:8080}"
REQUESTS="${2:-500}"
CONCURRENCY="${3:-20}"
TMP_RESULT="$(mktemp)"

echo "[loadtest] base_url=${BASE_URL} requests=${REQUESTS} concurrency=${CONCURRENCY}"

start_ts="$(date +%s)"

seq "${REQUESTS}" | xargs -I{} -P "${CONCURRENCY}" sh -c \
  'curl -s -o /dev/null -w "%{http_code}\n" "'"${BASE_URL}"'/api/v1/products"' > "${TMP_RESULT}"

end_ts="$(date +%s)"
duration=$((end_ts - start_ts))
if [[ "${duration}" -lt 1 ]]; then
  duration=1
fi

ok_count="$(grep -c '^200$' "${TMP_RESULT}" || true)"
fail_count=$((REQUESTS - ok_count))
rps=$((REQUESTS / duration))

echo "[loadtest] duration_seconds=${duration}"
echo "[loadtest] success=${ok_count} fail=${fail_count}"
echo "[loadtest] throughput_rps=${rps}"

rm -f "${TMP_RESULT}"
