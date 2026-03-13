# Ecommerce Practice (JDK 17)

一个以稳定技术栈为目标的电商后端实践项目。

## 技术栈

- JDK 17 (LTS)
- Spring Boot 3.4.x
- MySQL 8.x
- Redis 7.x
- MyBatis
- RabbitMQ（`prod` Profile）

## 当前进度

- [x] 阶段 1：项目骨架
- [x] 阶段 1：商品模块基础 API（内存实现）
- [x] 阶段 2：用户与登录态（JWT）
- [x] 阶段 3：购物车（内存实现）
- [x] 阶段 4：订单与库存预扣（基础版：内存锁 + 自动超时关单）
- [x] 阶段 5：支付与订单状态流转（模拟回调 + 幂等）
- [x] 阶段 6：MQ 异步化与可靠性（延迟消息 + 重试 + 死信）
- [x] 阶段 7：工程化收尾（Compose 一键启动 + 日志/监控 + 压测脚本）
- [ ] 阶段 8：生产化部署深化（进行中：已支持 MySQL/Redis 持久化 + RabbitMQ + MQ 运维数据落库 + prod 本地验收脚本）

详细计划见：`docs/development-plan.md`
API 文档见：`docs/phase1-api.md`、`docs/phase2-api.md`、`docs/phase3-api.md`、`docs/phase4-api.md`、`docs/phase5-api.md`、`docs/phase6-api.md`
工程化文档见：`docs/phase7-ops.md`
阶段 8 文档见：`docs/phase8-persistence.md`

## Prod 本地验收

在 `docker compose up --build -d` 后可执行：

- `./scripts/prod-acceptance.sh`

默认会串行校验：

- 基础容器状态
- `health` / `actuator`
- 入口与 404 行为
- 业务 smoke
- MQ smoke
- 轻量压测
- 订单流压测

## 飞书通知下游

你提供的 webhook 实际是飞书机器人地址，不是钉钉地址。`prod` 下现在支持两种通知模式：

- `mock`：默认值，继续回调项目内的 `/api/v1/notify/order-paid`
- `feishu`：向飞书机器人 webhook 发送文本消息

本地启用飞书 webhook 的方式：

```bash
APP_NOTIFY_PROVIDER=feishu \
APP_NOTIFY_FEISHU_WEBHOOK_URL='your-feishu-webhook' \
APP_NOTIFY_FEISHU_ENV_LABEL='prod' \
docker compose up --build -d
```

说明：

- `prod-acceptance.sh` 默认更适合 `mock` 模式
- 如果切到 `feishu`，`mq-smoke` 和订单流压测会真的向下游发送消息
- 飞书消息会包含服务名、环境、订单号、金额、交易号、支付时间、发送时间

## Swagger

启动应用后可访问：

- Swagger UI: `http://127.0.0.1:8080/swagger-ui/index.html`
- 账号：swagger_1773388400@test.com
- 密码：Test@123456
