# OrderFlow 文件说明

> 本文件按「根目录 → 脚本 → 文档 → 后端 → 前端 → 隐藏目录」的顺序，逐个说明每个文件/目录的作用，方便快速定位与维护。

---

## 一、根目录

| 文件 / 目录 | 作用 |
|---|---|
| `README.md` | 项目门面：一句话定位、技术亮点、架构图、快速开始、默认账号。新人/面试官看的第一份文档 |
| `FILES.md` | 本文件：逐文件说明项目结构 |
| `pom.xml` | Maven 构建配置：声明 Spring Boot 3.3.5 / MyBatis-Plus / Flyway / JWT 等依赖，`finalName=orderflow` 决定 jar 名 |
| `mvnw` / `mvnw.cmd` | Maven Wrapper 启动脚本（`mvnw` 用于 macOS/Linux，`mvnw.cmd` 用于 Windows）。自动下载并锁定 Maven 版本，无需本机装 mvn |
| `Makefile` | 命令入口：`make infra/build/run/stop/test/clean` 封装常用操作 |
| `docker-compose.yml` | 全栈编排：mysql / redis / rabbitmq / backend / frontend 五个服务，一键起依赖或整套 |
| `Dockerfile` | 后端运行镜像：基于 `eclipse-temurin:17-jre`，把 `target/orderflow.jar` 打成容器 |
| `src/` | 后端源码（Spring Boot，按领域分包），详见第三节 |
| `web/` | 前端源码（Vue3 + Vite + TS 子工程），详见第四节 |
| `target/` | Maven 构建产物（`orderflow.jar` 等），被 `.gitignore` 忽略，勿手动编辑 |
| `docs/` | 项目文档目录，详见第二节 |
| `scripts/` | 脚本目录，详见第二节 |

---

## 二、scripts/ 与 docs/

### 2.1 scripts/ —— 脚本

| 脚本 | 作用 |
|---|---|
| `build.sh` | 一键构建：后端 jar → 前端 dist → docker 镜像，三步串起来 |
| `run.sh` | 一键启动全栈（`docker compose up -d --build`），前端 8088 / 后端 /api |
| `run-host.sh` | 宿主机模式启动后端：本地起 jar（JVM 堆 512m），中间件仍用 docker |
| `stop.sh` | 停止全栈（`docker compose down`），释放容器资源 |
| `smoke-test.sh` | 端到端冒烟测试：起应用 → 健康检查 → 登录 → 下单 → 状态机 → 幂等 → 跨租户隔离 → RabbitMQ 接线，逐项 PASS/FAIL 汇总 |

### 2.2 docs/ —— 文档

| 文档 | 作用 |
|---|---|
| `PROJECT.md` | 工程化说明：技术栈、系统架构、核心流程、数据模型、设计取舍。面向面试官/维护者建立全貌 |
| `RUN.md` | 运行手册：环境要求、端口约定、两种启动方式、常用校验命令 |
| `INTERVIEW.md` | 面试手册：技术亮点逐条拆解 + 高频问答，简历项目描述底稿 |

---

## 三、后端 src/

### 3.1 入口与配置

| 路径 | 作用 |
|---|---|
| `src/main/java/com/orderflow/OrderFlowApplication.java` | Spring Boot 启动主类 |
| `src/main/resources/application.yml` | 应用配置（context-path=/api、数据源、Redis、RabbitMQ 等） |
| `src/main/resources/db/migration/` | Flyway 迁移脚本 `V1.1`~`V1.9`，版本化建表，杜绝手改库 |

### 3.2 领域分包（`src/main/java/com/orderflow/`）

| 包 | 作用 |
|---|---|
| `auth/` | Spring Security + JWT、种子账号 |
| `security/` | 安全相关（过滤器、鉴权） |
| `order/` | 订单状态机、超时自动取消、死信消费 |
| `product/` | 商品与库存：商品管理、库存调整、分布式锁防超卖（`InventoryService` 等在此包） |
| `category/` | 商品分类 |
| `store/` | 门店 |
| `customer/` | 顾客商城端 |
| `platform/` | 平台管理端（跨租户聚合统计） |
| `promotion/` | 营销满减 |
| `refund/` | 退款售后 |
| `notification/` | 异步通知、低库存告警 |
| `audit/` | 审计留痕 |
| `common/` | 健康检查、全局异常、多租户拦截等公共设施 |
| `config/` | Spring 配置类 |
| `domain/` | 领域模型/通用实体 |

> 注：多租户隔离由 MyBatis-Plus 的 `TenantLineHandler` 在 SQL 层自动注入 `tenant_id`，具体机制见 `docs/PROJECT.md`。

---

## 四、前端 web/

### 4.1 顶层文件

| 文件 | 作用 |
|---|---|
| `package.json` / `package-lock.json` | 前端依赖声明与锁文件 |
| `vite.config.ts` | Vite 构建配置（含 dev 代理 `/api` → 后端） |
| `tsconfig.json` / `tsconfig.node.json` | TypeScript 配置（应用 / Node 环境） |
| `index.html` | SPA 入口 HTML |
| `nginx.conf` | 生产容器内 nginx 配置，反代 `/api` 到后端 |
| `Dockerfile` | 前端镜像（nginx 托管 `dist` 静态产物） |
| `.dockerignore` | 前端镜像构建的忽略清单 |
| `dist/` | 前端构建产物（`npm run build` 生成，被 gitignore） |
| `node_modules/` | 前端依赖安装目录（被 gitignore） |

### 4.2 源码（`web/src/`）

| 目录 | 作用 |
|---|---|
| `api/` | 后端接口封装（Axios） |
| `views/` | 页面（仪表盘、商品、库存、订单管理等） |
| `layout/` | 布局组件 |
| `router/` | 路由 + 三端（平台/商家/顾客）门控 |
| `stores/` | Pinia 状态管理 |
| `components/` | 通用组件 |
| `types/` | TypeScript 类型定义 |
| `utils/` | 工具函数 |
| `constants/` | 常量定义 |
| `styles/` | 全局样式 / 设计系统（CSS 变量 + Element Plus 主题） |

---

## 五、隐藏目录

| 路径 | 作用 |
|---|---|
| `.gitignore` | Git 忽略规则：排除 `target/`、`node_modules/`、`dist/`、`.DS_Store` 等 |
| `.github/workflows/ci.yml` | CI：push 到 main/master 时编译后端 + 构建前端 + type-check |
| `.mvn/wrapper/maven-wrapper.properties` | Maven Wrapper 配置：锁定 Maven 3.9.9 |
| `.vscode/settings.json` | VS Code 项目配置：关闭 Java 自动编译、排除构建产物监听，降低资源占用 |

---

## 六、常用命令速查

```bash
make help      # 查看所有命令
make infra     # 只起中间件（MySQL/Redis/RabbitMQ）
make build     # 构建后端 jar + 前端 dist
make run       # 一键启动全栈
make test      # 端到端冒烟测试
make stop      # 停止并释放资源
```
