# 阶段 4 API 清单（订单与库存预扣）

以下接口均需要登录（`Authorization: Bearer <token>`）。

## 订单模块

- `POST /api/v1/orders` 从当前购物车创建订单（预扣库存）
- `POST /api/v1/orders/{orderId}/cancel` 取消订单（回补库存）
- `GET /api/v1/orders/{orderId}` 查询订单详情（若超时会自动关单）
- `GET /api/v1/orders` 查询我的订单列表

## 下单示例

先调用购物车接口加入商品后，直接请求：

```text
POST /api/v1/orders
```

## 取消订单示例

```text
POST /api/v1/orders/1001/cancel
```
