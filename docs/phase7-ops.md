# 阶段 7 工程化收尾（可运行、可观测、可压测）

## 一键启动（Docker Compose）

在项目根目录执行：

```bash
docker compose up --build -d
```

启动服务：

- 应用：`http://127.0.0.1:8080`
- MySQL：`127.0.0.1:3306`
- Redis：`127.0.0.1:6379`
- RabbitMQ 管理台：`http://127.0.0.1:15672`

停止：

```bash
docker compose down
```

## 观测能力

应用暴露如下监控端点：

- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/metrics`
- `GET /actuator/loggers`
- `GET /actuator/threaddump`

异步链路运维端点：

- `GET /api/v1/mq/process-logs?limit=50`
- `GET /api/v1/mq/dead-letters`
- `POST /api/v1/mq/dead-letters/{eventId}/retry`

## 日志追踪

- 服务端为每个请求生成或透传 `X-Request-Id`
- 响应头会返回 `X-Request-Id`
- 日志中打印 `requestId`，用于跨接口追踪

## 脚本

Smoke 验证：

```bash
./scripts/smoke.sh
```

MQ 链路 Smoke（支付后异步通知 + 运维接口）：

```bash
./scripts/mq-smoke.sh
```

基础压测（默认 500 请求，20 并发）：

```bash
./scripts/loadtest.sh
```

也可指定参数：

```bash
./scripts/loadtest.sh http://127.0.0.1:8080 2000 50
```
