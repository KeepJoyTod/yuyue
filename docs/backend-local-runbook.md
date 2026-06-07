# 后端本地启动与联调说明

## 1. 当前后端交付状态

已新增 `server/` Spring Boot 后端工程，当前实现范围：

- 健康检查：`GET /api/health`
- 手机号登录：`POST /api/auth/phone-login`
- 发送短信验证码：`POST /api/auth/sms/send`
- 微信登录 dev 适配：`POST /api/auth/wechat-login`
- 当前用户：`GET /api/users/me`
- 实名信息：`POST /api/users/real-name`
- 服务分类：`GET /api/service-categories`
- 服务列表：`GET /api/services`
- 服务详情：`GET /api/services/{id}`
- 门店列表：`GET /api/stores`
- 门店档期：`GET /api/stores/{id}/schedules`
- 创建预约：`POST /api/bookings`
- 订单列表：`GET /api/orders`
- 订单详情：`GET /api/orders/{id}`
- 模拟支付：`POST /api/orders/{id}/pay`
- 取消订单：`POST /api/orders/{id}/cancel`
- 完成订单：`POST /api/orders/{id}/complete`
- 用户侧隐藏订单：`DELETE /api/orders/{id}`
- 底片列表：`GET /api/negatives`
- 上传凭证：`POST /api/files/upload-token`
- 文件短期下载 URL：`GET /api/files/{id}/download-url`
- 支付回调框架：`POST /api/payments/wechat/callback`
- Admin 运营接口：`/api/admin/**`

## 2. 技术栈

- Java 17
- Maven 3.9.x
- Spring Boot 3.3.5
- Spring Web
- Spring JDBC
- Bean Validation
- Flyway
- H2 本地文件数据库
- MySQL Connector 生产预留

## 3. 本地启动

进入后端目录：

```powershell
cd D:\Java\class\projectKu\sure\yuyue\server
```

编译验证：

```powershell
mvn test
```

启动后端：

```powershell
mvn spring-boot:run
```

默认地址：

```text
http://localhost:8080
```

健康检查：

```powershell
Invoke-RestMethod -Uri 'http://localhost:8080/api/health'
```

## 4. 数据库

默认使用 H2 本地文件数据库：

```text
server/data/amber-film.mv.db
```

JDBC URL：

```text
jdbc:h2:file:./data/amber-film;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE
```

H2 控制台：

```text
http://localhost:8080/h2-console
```

连接信息：

```text
JDBC URL: jdbc:h2:file:./data/amber-film;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE
User: sa
Password: 空
```

数据库结构由 Flyway 自动执行：

```text
server/src/main/resources/db/migration/V1__init_schema.sql
server/src/main/resources/db/migration/V2__seed_data.sql
```

如果需要重置本地数据库，停止后端后删除：

```text
server/data/
```

然后重新执行 `mvn spring-boot:run`。

## 5. 环境变量

后端默认值适合本地开发，生产或联调环境可覆盖：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | 后端端口 |
| `DB_URL` | H2 本地文件 URL | 数据库连接 |
| `DB_USERNAME` | `sa` | 数据库账号 |
| `DB_PASSWORD` | 空 | 数据库密码 |
| `DB_DRIVER` | `org.h2.Driver` | JDBC Driver |
| `AMBER_TOKEN_SECRET` | `dev-secret-change-me` | token 签名密钥 |
| `AMBER_TOKEN_TTL_SECONDS` | `604800` | token 有效期 |
| `AMBER_AUTH_DEV_SMS_ENABLED` | `true` | 本地是否允许 `000000` dev 验证码 |
| `AMBER_SMS_TTL_SECONDS` | `300` | 短信验证码有效期 |
| `AMBER_ADMIN_TOKEN` | `dev-admin-token` | Admin API 本地 token |
| `AMBER_ALLOWED_ORIGIN_PATTERNS` | `*` | CORS 允许来源，多个值用英文逗号分隔 |
| `AMBER_STORAGE_PROVIDER` | `local` | 文件存储提供方，本地为 local |
| `AMBER_STORAGE_BUCKET` | `amber-film-dev` | 文件存储桶 |
| `AMBER_STORAGE_SIGNING_SECRET` | token secret | 文件 URL 签名密钥 |
| `AMBER_PAYMENT_DEV_CALLBACK_ENABLED` | `true` | 本地支付回调是否允许无签名 |
| `AMBER_PAYMENT_CALLBACK_SECRET` | token secret | 支付回调签名密钥 |

生产环境必须启用 `prod` 或 `production` profile，并覆盖以下变量：

```powershell
$env:SPRING_PROFILES_ACTIVE = 'prod'
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/amber_film?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
$env:DB_USERNAME = 'amber_app'
$env:DB_PASSWORD = '<strong-password>'
$env:DB_DRIVER = 'com.mysql.cj.jdbc.Driver'
$env:AMBER_TOKEN_SECRET = '<strong-auth-secret>'
$env:AMBER_ADMIN_TOKEN = '<strong-admin-token>'
$env:AMBER_ALLOWED_ORIGIN_PATTERNS = 'https://your-h5-domain.example.com'
$env:AMBER_AUTH_DEV_SMS_ENABLED = 'false'
$env:AMBER_STORAGE_PROVIDER = 'minio'
$env:AMBER_STORAGE_BUCKET = 'amber-film'
$env:AMBER_STORAGE_SIGNING_SECRET = '<strong-storage-secret>'
$env:AMBER_PAYMENT_DEV_CALLBACK_ENABLED = 'false'
$env:AMBER_PAYMENT_CALLBACK_SECRET = '<strong-payment-callback-secret>'
```

后端在生产 profile 下会启动防呆检查：禁止 H2、禁止 H2 console、禁止默认 token secret、禁止默认 admin token、禁止 CORS 使用 `*`，禁止 local/mock 存储、dev 短信和 dev 支付回调。如果配置不安全，应用会启动失败。

所有响应都会带 `X-Trace-Id`，调用方也可以传入 `X-Trace-Id` 便于串联日志。

## 6. 冒烟验证

### 6.1 查询服务

```powershell
Invoke-RestMethod -Uri 'http://localhost:8080/api/services'
```

### 6.2 查询门店档期

```powershell
Invoke-RestMethod -Uri 'http://localhost:8080/api/stores/1/schedules?serviceId=1&startDate=2026-06-08&days=3'
```

### 6.3 登录并创建预约

```powershell
$loginBody = @{ phone = '13800000000'; code = '000000' } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/auth/phone-login' -ContentType 'application/json' -Body $loginBody
$token = $login.data.token
$headers = @{ Authorization = "Bearer $token" }

$bookingBody = @{
  serviceId = 1
  storeId = 1
  scheduleId = 1
  contactName = '张同学'
  contactPhone = '13800000000'
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/bookings' -Headers $headers -ContentType 'application/json' -Body $bookingBody
```

### 6.4 查询订单

```powershell
Invoke-RestMethod -Method Get -Uri 'http://localhost:8080/api/orders' -Headers $headers
```

### 6.5 模拟支付

```powershell
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/orders/1/pay' -Headers $headers
```

## 7. 前端联调建议

前端当前运行在：

```text
http://localhost:10086/
```

后端当前运行在：

```text
http://localhost:8080/
```

下一步前端建议新增：

```text
client/src/api/request.ts
client/src/api/auth.ts
client/src/api/services.ts
client/src/api/stores.ts
client/src/api/orders.ts
client/src/api/negatives.ts
```

然后按以下顺序替换：

1. `client/src/data/services.ts` -> `GET /api/services`
2. 服务详情本地查找 -> `GET /api/services/{id}`
3. 页面内门店常量 -> `GET /api/stores`
4. 固定时段 -> `GET /api/stores/{id}/schedules`
5. mock 登录 -> `POST /api/auth/phone-login`
6. 本地创建订单 -> `POST /api/bookings`
7. 本地订单状态 -> `/api/orders` 系列接口
8. mock 底片 -> `GET /api/negatives`

## 8. 仍需外部资源的部分

- 当前手机号验证码已具备服务端发送/校验/消费记录；真实短信平台仍需接入供应商凭证。
- 当前 token 是轻量 HMAC 令牌，生产建议升级为标准 JWT + refresh token 或接入统一认证。
- 当前支付已有回调幂等框架；真实微信支付仍需商户号、证书、平台验签和补偿查询。
- 当前文件能力已有元数据和短期签名 URL；真实对象存储仍需接 MinIO/COS/OSS SDK 或网关。
- 当前已提供 Admin API 写能力；如需运营人员直接使用，还需要独立管理端页面。
