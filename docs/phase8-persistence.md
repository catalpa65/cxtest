# 阶段 8（进行中）：持久化与生产 Profile 切换

## 本阶段已完成

1. 引入 Profile 分层：
- `memory`（默认）：沿用内存仓储，便于本地快速开发与测试。
- `prod`：启用 MySQL + Redis + RabbitMQ。

2. 新增持久化仓储：
- 用户：`MySqlUserRepository`（MyBatis）
- 商品：`MySqlProductRepository`（MyBatis）
- 订单：`MySqlOrderRepository`（MyBatis，含订单项）
- 购物车：`RedisCartRepository`（`StringRedisTemplate`）

3. 新增数据库初始化脚本：
- `src/main/resources/db/schema.sql`
- `src/main/resources/db/data.sql`

4. Compose 接入 `prod` 运行参数：
- 容器内默认激活 `SPRING_PROFILES_ACTIVE=prod`
- 自动注入 MySQL/Redis/RabbitMQ 连接地址

5. MQ 从内存队列升级为 RabbitMQ（`prod`）：
- 新增 RabbitMQ 交换机/队列拓扑（主队列、延迟队列、死信队列）
- 支持延迟投递、失败重试、死信落库与手动重试
- `memory` 模式继续使用原 `AsyncEventBus`，保证开发与测试无外部依赖

6. 运维数据持久化（`prod`）：
- `mq_process_logs`：异步处理日志
- `mq_dead_letters`：死信记录
- `/api/v1/mq/process-logs` 与 `/api/v1/mq/dead-letters` 在 `prod` 下读取数据库数据

7. 通知网关外部化（`prod`）：
- 支付成功通知通过 `HttpOrderPaidNotificationGateway` 走 HTTP 调用
- 可通过 `app.notify.order-paid-url` 配置目标地址
- 通知头包含 `X-Notify-Timestamp`、`X-Notify-Id` 与 `X-Notify-Signature`（HMAC-SHA256）
- 接收端增加时间窗防重放与 `notifyId` 去重
- 默认提供 `POST /api/v1/notify/order-paid` 作为本地 mock 接收端点
- 新增 `feishu` provider，可通过 `app.notify.provider=feishu` + `app.notify.feishu-webhook-url` 切换到飞书机器人文本通知
- 飞书通知文案包含服务名、环境、订单号、金额、交易号、支付时间与发送时间

8. 新增 `prod` 本地验收脚本：
- `./scripts/prod-acceptance.sh`
- 串行覆盖 Compose 状态、健康检查、入口与 404、业务 smoke、MQ smoke、轻量压测、订单流压测
- 用于阶段 8 的本地集成回归，不替代 CI 或真实下游联调

## 运行方式

1. 默认内存模式（无外部依赖）：
```bash
mvn spring-boot:run
```

2. 生产模拟模式（MySQL + Redis + RabbitMQ）：
```bash
docker compose up --build
```

## 当前边界

1. 默认通知 provider 仍为 `mock`，真实生产环境需要切到真实下游地址并完成联调。
2. `feishu` provider 已支持，但压测类脚本会产生真实外发消息，使用时要控制流量。
