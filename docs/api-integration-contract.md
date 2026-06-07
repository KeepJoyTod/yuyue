# 前后端 API 对接契约

## 1. 当前替换范围

本项目当前主要使用本地数据和本地状态：

- 服务列表：`client/src/data/services.ts:11`
- 初始订单：`client/src/data/bookings.ts:3`
- 登录状态：`client/src/store/useAuthStore.ts:48`
- 订单状态：`client/src/store/useBookingStore.ts:75`
- 预约创建：`client/src/pages/booking/confirm/index.tsx:112`
- 订单状态更新：`client/src/pages/orders/index.tsx:176`

后续对接后，这些位置应逐步迁移到 API 请求层。

## 2. 前端请求层

建议新增：

```text
client/src/api/request.ts
client/src/api/auth.ts
client/src/api/services.ts
client/src/api/stores.ts
client/src/api/schedules.ts
client/src/api/orders.ts
client/src/api/negatives.ts
client/src/types/api.ts
```

`request.ts` 统一处理：

- `baseURL`
- `Authorization: Bearer <token>`
- `content-type`
- `401` 登录失效跳转
- 业务错误 toast
- 网络错误兜底
- H5 和小程序环境差异

## 3. 通用响应

成功：

```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "traceId": "20260607160000001"
}
```

失败：

```json
{
  "code": "VALIDATION_ERROR",
  "message": "手机号格式不正确",
  "data": null,
  "traceId": "20260607160000002"
}
```

分页：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "items": [],
    "page": 1,
    "pageSize": 20,
    "total": 0
  },
  "traceId": "20260607160000003"
}
```

## 4. API 清单

### 4.1 认证

#### POST `/api/auth/phone-login`

请求：

```json
{
  "phone": "13800000000",
  "code": "123456"
}
```

响应：

```json
{
  "token": "jwt-token",
  "user": {
    "id": "10001",
    "nickName": "微信用户",
    "phone": "13800000000",
    "realName": null,
    "avatarUrl": null
  }
}
```

#### GET `/api/users/me`

响应：

```json
{
  "id": "10001",
  "nickName": "微信用户",
  "phone": "13800000000",
  "realName": "张同学",
  "avatarUrl": null
}
```

#### POST `/api/users/real-name`

请求：

```json
{
  "realName": "张同学",
  "idCardNo": "仅开发环境可传明文，生产应加密或接认证平台"
}
```

响应：

```json
{
  "id": "10001",
  "realName": "张同学"
}
```

### 4.2 服务

#### GET `/api/service-categories`

响应：

```json
[
  { "id": "1", "code": "wedding", "name": "婚纱摄影" },
  { "id": "2", "code": "portrait", "name": "写真套系" }
]
```

#### GET `/api/services`

查询参数：

| 参数 | 说明 |
| --- | --- |
| categoryCode | 分类编码 |
| keyword | 搜索关键词 |
| storeId | 门店 ID |
| page | 页码 |
| pageSize | 每页数量 |

响应项：

```json
{
  "id": "20001",
  "name": "梦境 · 白纱系列",
  "categoryCode": "wedding",
  "categoryName": "婚纱摄影",
  "coverUrl": "https://cdn.example.com/services/wedding.jpg",
  "price": 4980,
  "priceCent": 498000,
  "durationMin": 180,
  "desc": "经典白纱 + 轻法式氛围",
  "tags": ["婚纱摄影", "白纱", "高口碑"],
  "rating": 4.9
}
```

#### GET `/api/services/{id}`

响应同服务列表项，可追加详情图、注意事项、样片列表。

### 4.3 门店与档期

#### GET `/api/stores`

查询参数：

| 参数 | 说明 |
| --- | --- |
| keyword | 搜索关键词 |
| tag | 标签 |
| serviceId | 支持指定套餐的门店 |

响应项：

```json
{
  "id": "30001",
  "name": "琥珀映画·静安旗舰店",
  "address": "静安区南京西路 1168 号嘉里中心 3F",
  "distanceKm": 0.8,
  "rating": 4.9,
  "reviews": 2341,
  "hours": "10:00-21:00",
  "tags": ["婚纱", "写真", "儿童"],
  "coverUrl": "https://cdn.example.com/stores/jingan.jpg",
  "hasSlotToday": true
}
```

#### GET `/api/stores/{id}/schedules`

查询参数：

| 参数 | 说明 |
| --- | --- |
| serviceId | 套餐 ID |
| startDate | 开始日期 |
| days | 查询天数 |

响应：

```json
[
  {
    "date": "2026-06-08",
    "slots": [
      {
        "scheduleId": "40001",
        "time": "10:00",
        "available": true,
        "remaining": 2
      }
    ]
  }
]
```

### 4.4 预约与订单

#### POST `/api/bookings`

请求：

```json
{
  "serviceId": "20001",
  "storeId": "30001",
  "scheduleId": "40001",
  "contactName": "张同学",
  "contactPhone": "13800000000"
}
```

响应：

```json
{
  "id": "50001",
  "orderNo": "ORD2026060700001",
  "status": "pending",
  "payStatus": "unpaid"
}
```

#### GET `/api/orders`

查询参数：

| 参数 | 说明 |
| --- | --- |
| status | pending/confirmed/completed/cancelled |
| page | 页码 |
| pageSize | 每页数量 |

响应项：

```json
{
  "id": "50001",
  "orderNo": "ORD2026060700001",
  "serviceName": "梦境 · 白纱系列",
  "serviceCoverUrl": "https://cdn.example.com/services/wedding.jpg",
  "storeName": "琥珀映画·静安旗舰店",
  "storeAddress": "静安区南京西路 1168 号嘉里中心 3F",
  "price": 4980,
  "priceCent": 498000,
  "durationMin": 180,
  "contactName": "张同学",
  "contactPhone": "13800000000",
  "date": "2026-06-08",
  "time": "10:00",
  "status": "pending",
  "payStatus": "unpaid",
  "createdAt": "2026-06-07T08:00:00Z"
}
```

#### POST `/api/orders/{id}/pay`

响应：

```json
{
  "id": "50001",
  "status": "confirmed",
  "payStatus": "paid"
}
```

#### POST `/api/orders/{id}/cancel`

响应：

```json
{
  "id": "50001",
  "status": "cancelled",
  "payStatus": "unpaid"
}
```

#### DELETE `/api/orders/{id}`

说明：仅隐藏用户侧展示，不物理删除业务订单。

### 4.5 底片

#### POST `/api/files/upload-token`

说明：当前为文件上传凭证第一版，占位本地开发存储；生产接入 MinIO/COS/OSS 后，`uploadUrl` 应替换为真实短期签名上传地址。

请求头：

```http
Authorization: Bearer <token>
```

请求：

```json
{
  "fileName": "negative-001.jpg",
  "contentType": "image/jpeg",
  "sizeByte": 1024000,
  "usage": "negative"
}
```

响应：

```json
{
  "storageProvider": "mock-local",
  "objectKey": "uploads/users/1/20260607/uuid-negative-001.jpg",
  "uploadUrl": "/api/files/mock-upload?objectKey=uploads%2Fusers%2F1%2F...",
  "assetUrl": "mock://amber-film/uploads/users/1/20260607/uuid-negative-001.jpg",
  "method": "PUT",
  "expiresAt": "2026-06-07T08:15:00Z"
}
```

#### GET `/api/negatives`

响应项：

```json
{
  "id": "60001",
  "orderId": "50001",
  "title": "梦境白纱-原片 001",
  "type": "original",
  "imageUrl": "https://cdn.example.com/negatives/001.jpg",
  "createdAt": "2026-06-08T10:00:00Z"
}
```

## 5. 前端替换清单

| 当前文件 | 当前行为 | 替换目标 |
| --- | --- | --- |
| `client/src/data/services.ts` | 本地套餐数据 | `GET /api/services` |
| `client/src/data/bookings.ts` | 本地订单样例 | 删除或仅开发模式保留 |
| `client/src/store/useAuthStore.ts` | mock 登录 | 登录 API + token |
| `client/src/store/useBookingStore.ts` | 本地订单真源 | 仅保留临时状态和缓存 |
| `client/src/pages/services/index.tsx` | 页面内门店常量 | `GET /api/stores` |
| `client/src/pages/services/detail/index.tsx` | 固定时间段 | 档期 API |
| `client/src/pages/booking/confirm/index.tsx` | 本地创建订单 | `POST /api/bookings` |
| `client/src/pages/orders/index.tsx` | 本地更新状态 | 订单 API |
| `client/src/pages/negatives/index.tsx` | mock 图片 | 底片 API |

## 6. 联调顺序

1. 健康检查接口。
2. 服务分类、套餐、门店查询。
3. 档期查询。
4. 手机号登录和当前用户。
5. 创建预约订单。
6. 订单列表和详情。
7. 支付、取消、完成。
8. 底片列表。
9. 文件上传和下载签名。
