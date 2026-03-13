# Local Acceptance Checklist

Date baseline: 2026-03-11

Shortcut:

```bash
./scripts/prod-acceptance.sh
```

The shortcut runs the main checks in this checklist with default load parameters.

## 1. Infrastructure up

Command:

```bash
docker compose ps
```

Expected:
- `mysql` status contains `healthy`
- `redis` status contains `healthy`
- `rabbitmq` status contains `healthy`
- `app` status is `Up`

## 2. Core health endpoints

Command:

```bash
curl -sS http://127.0.0.1:8080/api/v1/health
curl -sS http://127.0.0.1:8080/actuator/health
```

Expected:
- `code` is `OK`
- actuator `status` is `UP`
- components include `db`, `redis`, `rabbit` with `UP`

## 3. Entry path and 404 behavior

Command:

```bash
curl -i http://127.0.0.1:8080/
curl -i http://127.0.0.1:8080/this-path-should-not-exist
```

Expected:
- `/` returns `200` and service info JSON
- unknown path returns `404` with `code=NOT_FOUND`

## 4. Business smoke flow

Command:

```bash
./scripts/smoke.sh
```

Expected:
- output contains `[smoke] success`

Flow covered:
- register
- login
- add cart item
- create order
- mock payment callback
- complete order

## 5. MQ smoke flow

Command:

```bash
./scripts/mq-smoke.sh
curl -sS "http://127.0.0.1:8080/api/v1/mq/process-logs?limit=50"
curl -sS "http://127.0.0.1:8080/api/v1/mq/dead-letters"
```

Expected:
- output contains `[mq-smoke] success`
- process logs contain `ORDER_PAID_NOTIFY` with `status=SUCCESS`
- dead letters list is empty for normal flow

## 6. Lightweight load test

Command:

```bash
./scripts/loadtest.sh
```

Expected:
- `fail=0`
- throughput stable and non-zero

Reference result on 2026-03-11:
- requests: `500`
- concurrency: `20`
- success: `500`
- fail: `0`
- throughput: `125 rps`

Higher-load reference on 2026-03-11:
- requests: `2000`
- concurrency: `50`
- success: `2000`
- fail: `0`
- throughput: `111 rps`

Stress references on 2026-03-11:
- requests: `5000`
- concurrency: `100`
- success: `5000`
- fail: `0`
- throughput: `131 rps`

- requests: `10000`
- concurrency: `150`
- success: `10000`
- fail: `0`
- throughput: `138 rps`

- requests: `20000`
- concurrency: `200`
- success: `20000`
- fail: `0`
- throughput: `130 rps`

## 7. Order-flow load test

Command:

```bash
./scripts/order-loadtest.sh http://127.0.0.1:8080 300 30
./scripts/order-loadtest.sh http://127.0.0.1:8080 1000 50
./scripts/order-loadtest.sh http://127.0.0.1:8080 2000 80
```

Scope:
- register
- login
- add cart item
- create order
- payment callback
- complete order

Expected:
- `fail=0`
- success rate stays high under increased concurrency

References on 2026-03-11:
- requests: `300`
- concurrency: `30`
- success: `300`
- fail: `0`
- throughput: `10 rps`

- requests: `1000`
- concurrency: `50`
- success: `1000`
- fail: `0`
- throughput: `10 rps`

- requests: `2000`
- concurrency: `80`
- success: `2000`
- fail: `0`
- throughput: `11 rps`
