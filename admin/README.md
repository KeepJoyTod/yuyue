# Amber Film Admin

琥珀映画管理后台，按 Figma Make 的桌面 CMS 视觉实现，并接入当前后端的 `/api/admin/*` 管理接口。

## 启动

```powershell
cd admin
npm install
npm run dev
```

默认开发地址为 `http://localhost:5174`，接口代理到 `http://localhost:8080`。

默认管理令牌来自后端开发配置 `dev-admin-token`，可通过 `VITE_ADMIN_TOKEN` 覆盖。
