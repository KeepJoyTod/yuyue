# Amber Film Appointment

## 快速开始

### 一键启动（推荐）

在项目根目录双击运行：

```batch
start.bat
```

脚本会自动：
1. 检查 Java、Node、Maven 环境
2. 启动后端服务（端口 8080）
3. 启动前端服务（端口 10086）

### 一键停止

在项目根目录双击运行：

```batch
stop.bat
```

### 服务地址

- 📍 前端页面：http://localhost:10086
- 📍 后端 API：http://localhost:8080
- 📍 H2 控制台：http://localhost:8080/h2-console

---

## 项目结构

```text
yuyue/
  client/                 # Taro + React + TypeScript 前端
    config/               # Taro 构建配置
    src/
      api/                # 后端接口请求封装
      components/         # 通用 UI 组件
      data/               # 开发兜底/样例数据
      pages/              # 小程序/H5 页面
      store/              # Zustand 状态
      styles/             # 全局样式变量和兼容样式
      types/              # 前端领域类型和接口类型
      utils/              # 通用工具函数
    types/                # Taro 全局类型声明
  server/                 # Spring Boot 后端 API
    src/main/java/com/amberfilm/
      admin/              # Admin 运营接口
      auth/               # 登录、短信、用户认证
      booking/            # 预约创建
      catalog/            # 服务分类和套餐
      common/             # 通用响应、分页、异常
      config/             # Web、日志、生产防呆配置
      file/               # 文件上传凭证和下载 URL
      health/             # 健康检查
      negative/           # 底片列表
      order/              # 订单查询、支付、取消、完成
      payment/            # 支付回调
      store/              # 门店和档期
      user/               # 用户 DTO
    src/main/resources/   # 配置文件和 Flyway 迁移
    src/test/             # 后端冒烟测试
  docs/                   # 方案、接口、运行和生产化文档
  .github/workflows/      # CI
  docker-compose.yml      # 本地 MySQL/Redis/MinIO
  start.bat              # 一键启动脚本
  stop.bat               # 一键停止脚本
```

---

## 手动启动

### 前端

```powershell
cd client
npm install
npm run dev:h5
```

### 后端

```powershell
cd server
mvn spring-boot:run
```

### 常用命令

```powershell
cd client
npm run build:h5
```

```powershell
cd server
mvn test
```

