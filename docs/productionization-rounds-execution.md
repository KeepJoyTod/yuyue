# 7 轮生产化执行记录

## 本轮目标

围绕既定 7 轮计划，把项目从用户主链路可用继续推进到可运营、可生产预备状态。外部平台能力不伪造联调结果；缺少凭证时只实现后端可替换接口、本地 dev 适配器和生产配置防呆。

## 已落地

### 1. 对象存储与底片短期 URL

- 新增 `files` 元数据表，上传凭证会返回 `fileId`、`objectKey`、上传 URL 和短期下载 URL。
- 新增 `GET /api/files/{id}/download-url`，用户只能获取自己文件的下载 URL。
- 底片列表优先根据 `file_id -> files.object_key` 生成短期 `downloadUrl`；旧 `image_url` 数据继续兼容。

### 2. Admin 账号角色权限

- 新增 `admin_users` 表。
- `X-Admin-Token` 不再只是和环境变量硬比较，而是按 token digest 映射到管理员账号。
- 开发环境会用 `AMBER_ADMIN_TOKEN` 引导一个 `bootstrap-admin` 超级管理员。

### 3. Admin 写能力与审计

- 新增套餐、门店、档期、订单的 Admin 写接口。
- 写操作统一要求可写管理员角色，并写入 `admin_operation_logs`。
- 底片绑定支持 `fileId`，不再强制写永久图片 URL。

### 4. 短信/微信登录生产边界

- 新增 `POST /api/auth/sms/send`，验证码写入 `sms_codes` 并支持过期、消费。
- `POST /api/auth/phone-login` 增加验证码校验。
- 新增 `POST /api/auth/wechat-login` 的 dev 适配入口，真实微信 code 换 openid 仍需后续接官方凭证。

### 5. 支付回调幂等框架

- 新增 `POST /api/payments/wechat/callback`。
- 新增 `payment_events` 去重表，按 `channel + transaction_no + event_type` 幂等处理。
- 生产环境可关闭 dev callback，并通过回调签名密钥校验。

### 6. MySQL/Redis/MinIO 本地环境

- 新增根目录 `docker-compose.yml`，包含 MySQL 8、Redis 7、MinIO。
- 生产配置增加 storage、SMS、payment 相关环境变量。
- 生产启动防呆扩展到 local/mock 存储、dev 短信、dev 支付回调。

### 7. CI 与质量

- 新增 GitHub Actions：后端 `mvn test`、前端 `npm ci && npm run build:h5`。
- 后端测试已适配验证码和 `fileId` 底片绑定。
- 新增访问日志，记录请求 method、path、status、durationMs，并复用已有 traceId。

## 仍需真实外部资源

- 真实短信平台账号与签名模板。
- 微信登录 AppId/AppSecret 与小程序端 code 获取。
- 微信支付商户号、证书、回调地址和平台证书。
- 生产对象存储桶、访问密钥、私有桶/CORS/CDN 策略。
- 真实部署域名、HTTPS 证书和小程序合法域名。

## 验证命令

```powershell
cd D:\Java\class\projectKu\sure\yuyue\server
mvn test
```

```powershell
cd D:\Java\class\projectKu\sure\yuyue\client
npm run build:h5
```
