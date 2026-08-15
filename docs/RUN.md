# OrderFlow 运行手册

> 一条命令起服务，方便本地演示与面试展示。

## 环境要求

- JDK 17、Maven 3.9+、Node 18+、Docker（用于起中间件）

## 端口约定

| 服务 | 端口 |
| --- | --- |
| 后端 | 8080 |
| 前端（Vite dev） | 5173 |
| MySQL | 13306 |
| Redis | 16379 |
| RabbitMQ | 5672 / 15672 |

## 方式一：Docker Compose 起中间件（推荐）

```bash
# 1. 起依赖：MySQL / Redis / RabbitMQ
docker compose up -d

# 2. 后端（Flyway 自动建表 + 种子账号）
unset SERVER__PORT
mvn -DskipTests package
java -Dserver.port=8080 -jar target/orderflow.jar

# 3. 前端
cd web
npm install
npm run dev          # 打开 http://localhost:5173
```

前端通过 Vite 代理把 `/api` 转发到 `http://localhost:8080`。

## 方式二：本地已装 MySQL/Redis/RabbitMQ

修改 `src/main/resources/application.yml` 里的连接地址，然后只跑第 2、3 步。

## 默认账号

| 租户 | 账号 | 密码 |
| --- | --- | --- |
| 租户 A | `admin-a` | `admin123` |
| 租户 B | `admin-b` | `admin123` |

## 常用校验

```bash
# 健康检查（应返回 status=UP）
curl --noproxy '*' http://localhost:8080/api/health

# 登录拿 token
curl --noproxy '*' -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"tenantCode":"t-a","username":"admin-a","password":"admin123"}'
```

## 前端构建产物（部署用）

```bash
cd web && npm run build    # 产物在 web/dist
```

## 一键脚本

仓库提供 `scripts/run.sh` / `scripts/stop.sh` 封装上述步骤；容器化版本见 `docker-compose.yml` 的 `backend` / `frontend` 服务（分别映射 8088 / 8089）。
