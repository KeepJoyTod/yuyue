# Admin API 第一版对接契约

## 1. 范围

当前 Admin API 第一版以运营查询为主，并提供底片绑定这一项受控写操作，用于支撑后台管理端或临时运营工具读取核心业务数据、给订单绑定底片。

本轮不包含套餐、门店、档期、订单改状态等高风险写操作。后续写操作需要补充管理员账号、角色权限、完整操作审计和幂等策略后再落地。

## 2. 鉴权

所有 `/api/admin/**` 接口必须携带管理员 token 请求头：

```http
X-Admin-Token: dev-admin-token
```

本地默认值来自后端配置：

```yaml
amber:
  admin:
    token: ${AMBER_ADMIN_TOKEN:dev-admin-token}
```

生产环境必须通过 `AMBER_ADMIN_TOKEN` 覆盖默认值，且不应把固定 token 作为长期后台登录方案。后续建议升级为独立管理员账号、角色权限和审计日志。

鉴权失败响应：

```json
{
  "code": "ADMIN_AUTH_REQUIRED",
  "message": "管理员鉴权失败",
  "data": null
}
```

## 3. 响应规范

成功响应沿用用户侧统一结构：

```json
{
  "code": "OK",
  "message": "success",
  "data": {}
}
```

## 4. 接口清单

### 4.1 运营汇总

```http
GET /api/admin/summary
```

返回字段：

| 字段 | 说明 |
| --- | --- |
| `totalUsers` | 用户总数 |
| `totalServices` | 套餐总数 |
| `totalStores` | 门店总数 |
| `totalSchedules` | 档期总数 |
| `totalOrders` | 订单总数 |
| `pendingOrders` | 待支付订单数 |
| `confirmedOrders` | 已预约订单数 |
| `completedOrders` | 已完成订单数 |
| `totalNegatives` | 底片总数 |
| `visibleNegatives` | 可见底片数 |
| `paidRevenueCent` | 已支付订单金额合计，单位分 |

### 4.2 套餐列表

```http
GET /api/admin/services
```

返回套餐基础信息、分类、价格、时长、标签 JSON、评分、上下架状态和创建时间。

### 4.3 门店列表

```http
GET /api/admin/stores
```

返回门店名称、地址、距离、评分、评价数、营业时间、标签 JSON、封面和启用状态。

### 4.4 档期列表

```http
GET /api/admin/schedules
GET /api/admin/schedules?date=2026-06-08
```

查询参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `date` | 否 | 按服务日期过滤，格式 `YYYY-MM-DD` |

返回档期所属门店、套餐、日期、开始/结束时间、容量、已预约数和状态。

### 4.5 订单列表

```http
GET /api/admin/orders
GET /api/admin/orders?status=pending
```

查询参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `status` | 否 | 按订单状态过滤；不传或传 `all` 返回全部 |

返回订单号、用户手机号、套餐、门店、档期、联系人、金额、预约时间、订单状态、支付状态和用户隐藏状态。

### 4.6 底片列表

```http
GET /api/admin/negatives
GET /api/admin/negatives?userId=1
GET /api/admin/negatives?orderId=1
GET /api/admin/negatives?userId=1&orderId=1
```

查询参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `userId` | 否 | 按用户过滤 |
| `orderId` | 否 | 按订单过滤 |

返回底片所属用户、订单、标题、类型、图片地址、状态和创建时间。

### 4.7 创建底片绑定

```http
POST /api/admin/negatives
```

请求：

```json
{
  "orderId": 1,
  "title": "梦境白纱-精修 001",
  "type": "retouched",
  "imageUrl": "mock://amber-film/uploads/users/1/20260607/demo.jpg",
  "status": "visible"
}
```

字段说明：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `orderId` | 是 | 绑定的订单 ID，后端会根据订单自动取用户 ID |
| `title` | 是 | 底片标题 |
| `type` | 是 | `original` 原片，`retouched` 精修 |
| `imageUrl` | 是 | 底片资源地址；本地第一版可使用上传凭证返回的 `assetUrl` |
| `status` | 否 | `visible` 或 `hidden`，默认 `visible` |

创建成功后会写入 `admin_operation_logs`，动作类型为 `NEGATIVE_CREATE`。

### 4.8 审计日志

```http
GET /api/admin/audit-logs
GET /api/admin/audit-logs?action=NEGATIVE_CREATE
GET /api/admin/audit-logs?targetType=negative&targetId=1
```

查询参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `action` | 否 | 按操作动作过滤 |
| `targetType` | 否 | 按目标类型过滤 |
| `targetId` | 否 | 按目标 ID 过滤 |

返回字段包含操作动作、目标类型、目标 ID、详情 JSON 和创建时间。日志中只保存管理员 token 摘要，不返回 token 明文。

## 5. 本地验证示例

```powershell
$headers = @{ "X-Admin-Token" = "dev-admin-token" }
Invoke-RestMethod -Uri "http://localhost:8080/api/admin/summary" -Headers $headers
Invoke-RestMethod -Uri "http://localhost:8080/api/admin/orders?status=pending" -Headers $headers
Invoke-RestMethod -Uri "http://localhost:8080/api/admin/schedules?date=2026-06-08" -Headers $headers
Invoke-RestMethod -Uri "http://localhost:8080/api/admin/audit-logs?action=NEGATIVE_CREATE" -Headers $headers
```
