# 阶段 2 API 清单（用户与鉴权）

## 用户模块

- `POST /api/v1/users/register` 注册
- `POST /api/v1/users/login` 登录
- `GET /api/v1/users/me` 当前登录用户信息（需 Bearer Token）

## 注册示例

请求体：

```json
{
  "email": "demo@example.com",
  "password": "Password123",
  "nickname": "demo"
}
```

## 登录示例

请求体：

```json
{
  "email": "demo@example.com",
  "password": "Password123"
}
```

响应 `data.token` 用于后续请求头：

```text
Authorization: Bearer <token>
```
