# Jade Platform

> **温润如玉，稳定如石** — 基于 Quarkus 3.39 LTS + Vue 3.5 的企业级全栈开发底座。

Jade 是面向企业级 Java 应用的**全栈开发底座**，采用前后端分离 + Monorepo 架构。后端基于 Quarkus 3.39 LTS（云原生、GraalVM 原生镜像 84ms 启动），前端基于 Vue 3.5 + Vite 7 + Element Plus 2.14。

## 🌟 核心特性

- ⚡ **极致启动**：GraalVM 原生镜像 **84ms 启动 / 291MB 镜像 / 160MB 内存**
- 🛡 **企业级安全**：JWT (RS256) + RBAC + **多租户 + data-scope 数据权限 (ALL/DEPT/SELF)**
- 🎨 **统一规范**：统一响应 `R<T>` / 全局异常 / OpenAPI 自动生成前端 SDK
- 🔐 **注解式**：分布式锁 `@RedisLock` / 操作日志 `@Log` / 字段加密 `@Encrypted`
- 📊 **生产级监控**：Prometheus + Grafana 25-panel 业务仪表板
- 🐳 **容器化**：docker-compose 一键起 PG + Redis + Prometheus + Grafana
- 🧪 **测试完备**：69 个测试用例 (4 security + 39 redis + 4 auth + 8 data-scope + 6 role + 8 user)

## 📁 目录结构

```
Jade/
├── backend/                  # Quarkus 3.39 LTS 多模块工程
│   ├── jade-dependencies/    # BOM 依赖管理
│   ├── jade-common/          # R/PageResult/异常/ResultCode
│   ├── jade-web/             # TraceId 透传 + CORS filter + 启动指标
│   ├── jade-security/        # JWT 签发/验证 + AES-256-GCM 字段加密 + 多租户
│   ├── jade-log/             # @OperateLog 注解 + 切面
│   ├── jade-redis/           # 8 种分布式锁 (Fair/RW/Reentrant/Semaphore/CountDownLatch/...)
│   ├── jade-oss/             # 对象存储抽象
│   ├── jade-codegen/         # 代码生成器
│   ├── jade-spring-bridge/   # Spring 兼容桥
│   ├── jade-admin/           # RBAC + 系统管理 (生产业务) — 12 controller
│   └── jade-demo/            # 唯一 runnable，聚合所有功能
├── frontend/                 # Vue 3.5 + Vite 7 + TS 5.9 + Element Plus 2.14
├── monitoring/               # Prometheus 配置 + Grafana dashboard JSON
├── Makefile                  # 一键命令 (make help)
├── docker-compose.yml        # PG + Redis
├── docker-compose.monitoring.yml  # Prometheus + Grafana
└── Dockerfile(.native)      # JVM / GraalVM Native 构建
```

---

## 🚀 5 分钟上手

### 0. 环境要求

| 工具 | 版本 | 验证 |
|---|---|---|
| **JDK** | **21 LTS** | `make env` |
| Maven | 3.9+ | 自带 `mvnw` |
| Node.js | **22.18+** | `node -v` (orval 8 要求) |
| Docker | 任意 | `docker ps` |

> **macOS JDK 路径**：`/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home`

### 1. 启动中间件 (PG + Redis + Prometheus + Grafana)

```bash
# 基础 (PG + Redis)
make up

# 监控 (Prometheus + Grafana)
docker-compose -f docker-compose.monitoring.yml up -d
```

### 2. 启动后端

```bash
make dev
# 或: cd backend && ./mvnw -B quarkus:dev -pl jade-demo
```

**首次启动会**：
- Flyway 跑 V1~V8 建表 + 灌默认数据 (admin/admin123, user/tenant/...)
- 输出 `jade-demo started in 4.xxs. Listening on: http://localhost:8080`

**关键端点**：
- API: http://localhost:8080
- OpenAPI: http://localhost:8080/q/openapi
- Swagger UI: http://localhost:8080/q/swagger-ui
- 健康检查: http://localhost:8080/q/health
- 业务指标: http://localhost:8080/q/metrics
- Grafana: http://localhost:3001 (admin/admin)

### 3. 启动前端 (新开终端)

```bash
make fe-install
make fe-gen     # 首次: 从后端 OpenAPI 生成 TS SDK
make fe-dev
```

访问: http://localhost:5173

### 4. 登录

| 用户名 | 密码 | 角色 | data-scope |
|---|---|---|---|
| `admin` | `admin123` | 超级管理员 | ALL (跨租户) |
| `tenant` | `admin123` | 租户管理员 | DEPT (本部门) |
| `user` | `admin123` | 普通用户 | SELF (仅自己) |
| `alice` / `bob` / `charlie` / `dave` | `admin123` | 普通用户 / 租户 | SELF / DEPT |

---

## 🔧 常用命令速查

| 命令 | 作用 |
|---|---|
| `make help` | 查看所有命令 |
| `make up` / `down` | 起/停 PG + Redis |
| `make dev` | 后端 dev 模式 (热重载) |
| `make fe-dev` | 前端 dev server |
| `make all-start` / `all-stop` | 一键起/停全栈 (后台) |
| `make backend-test` | 跑后端所有测试 (69 个) |
| `make backend-package` | 打包 JVM jar |
| `make backend-native` | 打包 GraalVM Native (7 分钟) |
| `make docker-build` / `docker-native` | 构 Docker 镜像 |
| `make reset-db` | 重置数据库 (重新跑 Flyway) |
| `make health` / `make login` | 快速验证 |

---

## 🏗 架构

### 后端 (Quarkus 3.39 LTS + Java 21)

**12 模块 Monorepo** — 共享 BOMS + 按需引用：

```
jade-demo (runnable, 唯一部署单元)
  ├─ jade-admin (RBAC + 系统管理)
  ├─ jade-security (JWT + 加密 + 多租户)
  ├─ jade-common (R/PageResult/异常)
  ├─ jade-web (CORS + 启动指标)
  ├─ jade-log (@OperateLog 注解)
  ├─ jade-redis (8 种分布式锁)
  ├─ jade-oss / jade-codegen / jade-spring-bridge
  └─ jade-dependencies (BOM)
```

**16 个 controller** (其中 12 个 admin 业务 + 4 个 demo 业务)：
- **Admin**: Auth, User, Role, Menu, Dept, Dict, Notice, Log, Oss, Task, Config, Tenant
- **Demo**: Order, Patient, Inventory, LockDemo, Metrics, Export

**关键能力**：
- **JWT (RS256)**: `mp.jwt.verify.issuer` + `jade.jwt.issuer` 双向认证
- **多租户**: JWT claim 携带 `tenantId` + `TenantFilter` 严格校验
- **data-scope 数据权限**: ALL/DEPT/DEPT_AND_CHILD/SELF (4 种)
- **字段加密**: AES-256-GCM, `@Encrypted` 注解 + `EncryptedAttributeConverter`
- **分布式锁**: 8 种实现 (Fair / RW / Reentrant / Semaphore / CountDownLatch / Renewer / Multi-lock)
- **注解式切面**: `@Log`(操作日志) / `@OperateLog`(系统日志) / `@RedisLock`(分布式锁) / `@Encrypted`(字段加密)
- **业务指标**: 8 个自定义 Micrometer Counter/Histogram (登录/订单/患者/业务计时)
- **Flyway**: V1~V8 渐进式迁移 (V8 加入多租户 + data-scope 测试数据)

### 前端 (Vue 3.5 + Vite 7 + Element Plus 2.14)

**12 个核心页面 + 8 个 admin 管理页**：
- 仪表盘 (业务指标大屏)
- 用户/角色/菜单/部门/字典/公告 (CRUD + 树形 + 弹窗)
- 操作日志/登录日志 (审计)
- 分布式锁演示 / 业务指标
- 个人中心 / 平台能力展示

**13 admin 测试 + data-scope E2E + role-menu 关联**

### 基础设施 (Docker Compose)

| 容器 | 端口 | 镜像 | 用途 |
|---|---|---|---|
| jade-postgres | 5432 | postgres:16-alpine | 主库 (10 + 11 张表) |
| jade-redis | 6380 | redis:7-alpine | 分布式锁 + 缓存 + 幂等 |
| jade-prometheus | 9090 | prom/prometheus:v2.55 | 抓 `/q/metrics` (200h 留存) |
| jade-grafana | 3001 | grafana:11.4 | 25-panel 业务仪表板 |

---

## 📊 性能数据

| 指标 | JVM | **GraalVM Native** | 提升 |
|---|---|---|---|
| 启动时间 | 4.2s | **84ms** | **50x** |
| 镜像大小 | 584MB | **291MB** | **2x** |
| 运行内存 | 200-400MB | **160MB** | 2-3x |
| 请求延迟 | 5-10ms | **2.5ms** | 2-4x |
| 编译时间 | 30s | 7-8 min | (一次性) |

---

## 🧪 测试

**69 个测试用例全绿**:

```bash
make backend-test

# 输出:
# Tests run: 4, Failures: 0, Errors: 0, Skipped: 0   (jade-security)
# Tests run: 39, Failures: 0, Errors: 0, Skipped: 0  (jade-redis, 8 类锁)
# Tests run: 4, Failures: 0, Errors: 0, Skipped: 0   (AuthControllerTest)
# Tests run: 8, Failures: 0, Errors: 0, Skipped: 0   (DataScopeE2ETest)
# Tests run: 6, Failures: 0, Errors: 0, Skipped: 0   (RoleMenuTest)
# Tests run: 8, Failures: 0, Errors: 0, Skipped: 0   (UserControllerTest)
```

---

## 🐳 部署

### Native 镜像 (推荐生产)

```bash
make docker-native
# 输出: jade-demo:native (291MB)

docker run --rm -p 8080:8080 \
  --network quarkus_default \
  -e DB_URL=jdbc:postgresql://jade-postgres:5432/jade \
  -e DB_USERNAME=postgres -e DB_PASSWORD=postgres \
  -e REDIS_URL=redis://jade-redis:6379 \
  -e JADE_JWT_SECRET=... -e JADE_CRYPTO_MASTER_KEY=... \
  -e JADE_JWT_ISSUER=jade-platform \
  jade-demo:native
```

### JVM 镜像

```bash
make docker-build
docker run --rm -p 8080:8080 jade-demo:1.0.0
```

---

## 🛠 排错指南

### Q1: `release: 21 requires JDK 21+`

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
```

### Q2: `quarkus:dev` 启动报 `Hot reloadable dependency ... has not been compiled yet`

```bash
cd backend && ./mvnw -B -DskipTests install
```

### Q3: `Bind for 0.0.0.0:8080 failed: port is already allocated`

测试 JVM 跟生产 JVM 抢端口:
```bash
docker stop jade-demo  # 或 pkill -f "quarkus:dev"
```

### Q4: `Bind for 0.0.0.0:6379 failed: port is already allocated`

本机有 Redis。`docker-compose.yml` 用 `6380:6379`,`application.yml` 用 `redis://localhost:6380`。

### Q5: Maven 找不到 3.39.1 的某些包 (aliyun 镜像同步延迟)

`~/.m2/settings.xml` 的 mirror 加 central fallback:
```xml
<mirror>
  <id>aliyun</id>
  <mirrorOf>*,!central</mirrorOf>
  <url>https://maven.aliyun.com/repository/public</url>
</mirror>
```

### Q6: Flyway 报 `relation "sys_user" does not exist`

`make reset-db` 重置 DB 后重启。

### Q7: 登录返回 "用户名或密码错误"

默认密码 `admin123` (BCrypt `$2a$10$` 前缀, Java `at.favre.lib.bcrypt` 兼容)。

### Q8: 前端 `npm install` 巨慢

```bash
npm config set registry https://registry.npmmirror.com
```

### Q9: macOS Apple Silicon 跑 native 镜像失败

GraalVM native 镜像跟芯片架构绑定。在 M1/M2 上构建只能在 M1/M2 上跑。Linux x86_64 上构建不能在 Mac 上跑。

### Q10: Native build 报 `Could not find artifact ... in aliyun`

更新 `backend/jade-demo/settings-docker.xml` mirror 加 `mirrorOf="*,!central"` (跟 Q5 一样)。

---

## 📦 业务项目怎么用 Jade

### 1. 后端 pom.xml

```xml
<parent>
    <groupId>com.jade</groupId>
    <artifactId>jade-dependencies</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</parent>

<dependencies>
    <dependency>
        <groupId>com.jade</groupId>
        <artifactId>jade-web</artifactId>
    </dependency>
    <dependency>
        <groupId>com.jade</groupId>
        <artifactId>jade-security</artifactId>
    </dependency>
</dependencies>
```

### 2. 前端从脚手架拉

```bash
npx degit jade-platform/frontend my-app-frontend
cd my-app-frontend && npm install
```

---

## 🤝 贡献

1. Fork
2. 创建 feature 分支 (`git checkout -b feature/amazing`)
3. 提交 (`git commit -m 'feat: add amazing'`)
4. 推送 (`git push origin feature/amazing`)
5. 提 PR

## 📄 许可证

MIT License
