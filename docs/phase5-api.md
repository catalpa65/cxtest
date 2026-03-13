# 阶段 5 API 清单（支付与订单状态流转）

## 支付回调（模拟）

- `POST /api/v1/payments/mock-callback` 模拟支付平台回调（无需登录）

请求体：

```json
{
  "orderId": 1001,
  "transactionId": "txn-20260310-0001",
  "paidAmount": 6999.00
}
```

返回字段 `data.idempotent` 表示是否命中幂等回调。

## 订单状态流转

- `POST /api/v1/orders/{orderId}/complete` 用户确认完成订单（需登录）

状态流转：

- `PENDING_PAYMENT -> PAID`（支付回调）
- `PENDING_PAYMENT -> CANCELED`（主动取消）
- `PENDING_PAYMENT -> TIMEOUT_CANCELED`（超时自动关单）
- `PAID -> COMPLETED`（确认完成）
