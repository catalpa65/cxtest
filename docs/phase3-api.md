# 阶段 3 API 清单（购物车）

以下接口均需要登录（`Authorization: Bearer <token>`）。

## 购物车模块

- `GET /api/v1/cart` 查询当前用户购物车
- `POST /api/v1/cart/items` 加入购物车
- `PUT /api/v1/cart/items/{productId}` 修改商品数量
- `DELETE /api/v1/cart/items/{productId}` 删除购物车商品

## 加入购物车示例

请求体：

```json
{
  "productId": 1,
  "quantity": 2
}
```

## 修改数量示例

请求体：

```json
{
  "quantity": 3
}
```
