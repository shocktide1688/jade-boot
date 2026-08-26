# Jade Platform

> **温润如玉，稳定如石** — 基于 Quarkus 3.15 + Vue 3 的全栈开发底座。

Jade 是面向企业级 Java 应用的**全栈开发底座**，采用前后端分离 + Monorepo 架构。后端基于 Quarkus（云原生、GraalVM 原生镜像），前端基于 Vue 3 + Vite + Element Plus。

## 🌟 核心特性

- ⚡ **极致启动**：后端支持 GraalVM 原生镜像，毫秒级启动
- 🎨 **统一规范**：统一响应 / 全局异常 / OpenAPI 自动化前端 SDK
- 🔐 **开箱即用**：JWT 鉴权 / RBAC / 限流 / 分布式锁
- 🛠️ **代码生成**：DB → Entity / DTO / Controller 一键生成
- 📦 **多模块 starter**：`jade-common / web / security / log / redis / oss / codegen` 按需引用
- 🐳 **容器友好**：docker-compose 一键起 PG + Redis
- 🚀 **已验证可跑**：登录、用户分页、OpenAPI、Flyway 端到端通过

## 📁 目录结构

```
Jade/
├── backend/                  # Quarkus 多模块工程
│   ├── jade-dependencies/    # BOM（业务项目 import 此 BOM 拿全部 starter）
│   ├── jade-common/          # 统一响应 / 异常 / 分页 / 错误码
│   ├── jade-web/             # TraceId 透传 / OpenAPI
│   ├── jade-security/        # JwtUtil / UserContext / @RequiresRoles
│   ├── jade-log/             # 日志（占位）
│   ├── jade-redis/           # 分布式锁（SET NX EX + Lua 释放）
│   ├── jade-oss/             # 对象存储抽象
│   ├── jade-codegen/         # 代码生成器
│   └── jade-demo/            # 演示项目（活文档）
├── frontend/                 # Vue 3 + Vite
├── scaffold/                 # 脚手架（archetype）
├── docs/                     # 文档
├── Makefile                  # 一键命令（make help 查看）
└── docker-compose.yml        # PG + Redis
```

详细结构：[STRUCTURE.md](./STRUCTURE.md) · 架构设计：[ARCHITECTURE.md](./ARCHITECTURE.md)

---

## 🚀 5 分钟上手

### 0. 环境要求

| 工具 | 版本 | 说明 |
|---|---|---|
| **JDK** | **21+** | 必须，Quarkus 3.15 需要 JDK 21+ |
| Maven | 3.9+ | 自带 `mvnw` 无需装 |
| Node.js | 20+ | 前端 |
| Docker | 任意 | 起 PG + Redis |

> **macOS JDK 路径**（Makefile 默认）：`/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home`
> 可用 `make env` 检查环境

### 1. 启动中间件

```bash
make up
# 或：docker-compose up -d
```

### 2. 启动后端

```bash
make dev
# 或：cd backend && ./mvnw -B quarkus:dev -pl jade-demo -Dquarkus.analytics.disabled=true
```

**首次启动会**：
- 自动跑 Flyway 建表 + 灌默认 admin 用户
- 输出 `jade-demo 1.0.0 on JVM started in 2.xxs. Listening on: http://localhost:8080`

**关键端点**：
- API：http://localhost:8080
- OpenAPI：http://localhost:8080/q/openapi
- Swagger UI：http://localhost:8080/q/swagger-ui
- 健康检查：http://localhost:8080/q/health

### 3. 启动前端（新开终端）

```bash
make fe-install
make fe-gen     # 首次：拉后端 OpenAPI 生成 SDK
make fe-dev
# 或：
# cd frontend && npm install && npm run gen:api && npm run dev
```

访问：http://localhost:5173

### 4. 登录

```
用户名：admin
密码：admin123
```

---

## 🔧 常用命令速查

| 命令 | 作用 |
|---|---|
| `make help` | 查看所有命令 |
| `make env` | 检查环境 |
| `make up` / `down` | 起/停中间件 |
| `make dev` | 后端 dev 模式（热重载） |
| `make fe-dev` | 前端 dev server |
| `make all-start` | 一键起全栈（后台运行） |
| `make all-stop` | 一键停全栈 |
| `make backend-package` | 打包 JVM jar |
| `make backend-native` | 打包 GraalVM Native |
| `make docker-build` | 构 Docker 镜像（JVM） |
| `make docker-native` | 构 Native 镜像 |
| `make reset-db` | 重置数据库（重新跑 Flyway） |
| `make health` | 健康检查 |
| `make login` | 测登录接口 |

---

## 🧪 已验证可用的接口

```bash
# 1. 登录
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 返回：
# {"code":0,"message":"success","data":{"accessToken":"eyJ...",...}}

# 2. 用户分页
curl "http://localhost:8080/api/v1/users?page=1&size=10"

# 3. 健康检查
curl http://localhost:8080/q/health
```

---

## 📜 业务项目怎么用 Jade

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

## 🐳 部署

### JVM 镜像

```bash
make docker-build
make docker-run
```

### Native 镜像（推荐生产）

```bash
# 1. 在 Apple Silicon / Linux 上构建
make backend-native
# 2. 构 Docker 镜像
make docker-native
# 3. 跑（启动 < 100ms，镜像 < 100MB）
docker run --rm -p 8080:8080 jade-demo:native
```

---

## 🛠 排错指南

### Q1: 构建报 `release: 21 requires JDK 21+`

A: JAVA_HOME 没指向 JDK 21。修改或导出：
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
```

或在 IDE 里设 Project SDK 为 21。

### Q2: `quarkus:dev` 启动报 `Hot reloadable dependency ... has not been compiled yet`

A: 先 `cd backend && ./mvnw -B -DskipTests install` 把所有模块 install 到本地仓库。

### Q3: Docker Desktop 报 `Bind for 0.0.0.0:6379 failed: port is already allocated`

A: 你本地已经有 Redis 在 6379。`docker-compose.yml` 里把 redis 端口改成 `6380:6379`，application.yml 里 `quarkus.redis.hosts` 改 `redis://localhost:6380`。

### Q4: `quarkus-redis-client` 报 Testcontainers 失败

A: 同上，把 `quarkus.redis.devservices.enabled: false` 显式关掉，用本地 Redis。

### Q5: Flyway 启动报 `relation "sys_user" does not exist`

A: 第一次启动会自动建。如果报这个，说明 `V1__init.sql` 没执行。检查：
1. `quarkus.flyway.migrate-at-start: true` 是否配置
2. SQL 路径 `classpath:db/migration` 是否对
3. `make reset-db` 重置一下

### Q6: Orval 报 `ECONNREFUSED 127.0.0.1:8080`

A: 后端没启动。Orval 需要后端在 8080 端口跑着。

### Q7: 登录返回 "用户名或密码错误"

A: 默认密码是 `admin123`。如果你改了 V1 SQL 里的 hash，要对应改密码。

### Q8: 前端 npm install 巨慢

A: 换镜像：
```bash
npm config set registry https://registry.npmmirror.com
```

### Q9: macOS Apple Silicon 跑 native 镜像失败

A: GraalVM native 镜像跟芯片架构绑定。在 M1/M2 上构建只能在 M1/M2 上跑。在 Linux x86_64 上构建不能在 Mac 上跑。

---

## 🤝 贡献

1. Fork
2. 创建 feature 分支 (`git checkout -b feature/amazing`)
3. 提交 (`git commit -m 'feat: add amazing'`)
4. 推送 (`git push origin feature/amazing`)
5. 提 MR

## 📄 许可证

MIT License
