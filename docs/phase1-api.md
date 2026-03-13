# 阶段 1 API 清单

## 健康检查

- `GET /api/v1/health`

## 商品模块

- `POST /api/v1/products` 创建商品
- `GET /api/v1/products` 商品列表
- `GET /api/v1/products/{id}` 商品详情
- `PUT /api/v1/products/{id}/on-shelf` 商品上架
- `PUT /api/v1/products/{id}/off-shelf` 商品下架

## 创建商品示例

请求体：

```json
{
  "name": "MacBook Pro 14",
  "description": "M4 Pro",
  "price": 14999.00,
  "stock": 50
}
```
