# 阶段 6 API 清单（MQ 异步化与可靠性）

当前接口在两个运行模式下保持一致：
- `memory`（默认）：使用内存可靠队列模拟 MQ 语义（延迟、重试、死信、手动重试）。
- `prod`：使用 RabbitMQ 承载同一套语义。

## 异步链路

- 下单成功后发布 `ORDER_DELAY_CLOSE` 延迟事件（到期自动关单）
- 支付成功后发布 `ORDER_PAID_NOTIFY` 异步通知事件
- `prod` 通知请求头：`X-Notify-Timestamp`、`X-Notify-Id`、`X-Notify-Signature`

## MQ 观测与运维接口

- `GET /api/v1/mq/process-logs?limit=50` 查询处理日志（链路追踪）
- `GET /api/v1/mq/dead-letters` 查询死信队列
- `POST /api/v1/mq/dead-letters/{eventId}/retry` 手动重试死信

## 配置项

`application.yml`：

```yaml
app:
  mq:
    retry-max-attempts: 3
    retry-delay-millis: 500
    process-log-retain-size: 300
  notify:
    order-paid-url: "http://127.0.0.1:8080/api/v1/notify/order-paid"
    sign-secret: "replace-notify-sign-secret"
    connect-timeout-millis: 2000
    read-timeout-millis: 3000
    replay-window-seconds: 300
    replay-cache-max-size: 10000
```
