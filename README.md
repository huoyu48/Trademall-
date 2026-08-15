# OrderFlow

> 一个**多租户订单履约平台**：一套系统服务多个商家，商家管理商品与订单、顾客在线下单、平台全局治理，覆盖下单、扣库存、订单流转、异步通知的完整链路。

## 核心特性

- **多租户数据隔离**
  基于 MyBatis-Plus 多租户拦截器在 SQL 层自动注入 `tenant_id`，租户之间数据彻底隔离，业务代码零侵入。

- **高并发防超卖**
  Redis 分布式锁锁住商品维度 + 数据库原子扣减双重防护，扣库存与建订单在同一事务边界内，要么都成、要么都回滚。

- **订单状态机**
  订单生命周期 `CREATED → CONFIRMED → SHIPPED → COMPLETED`，终态 `CANCELLED`。状态只能沿允许的边流转，非法跳转被拒绝，流转全程可审计。

- **异步解耦 + 可靠通知**
  下单后发 RabbitMQ 事件异步消费；消费失败重试 3 次，仍失败则进入死信队列归档，毒消息既不丢也不阻塞主队列。

- **订单超时自动取消**
  定时扫描超时未支付订单，自动取消并回补库存。

- **可观测性**
  `/api/health` 端点逐项探活 MySQL / Redis / RabbitMQ；通知与死信结构化落库，便于排查。

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Spring Boot 3.3.5 · Java 17 · MyBatis-Plus · Spring Security + JWT · Flyway |
| 存储 / 中间件 | MySQL · Redis（分布式锁）· RabbitMQ（异步 + 死信） |
| 前端 | Vue 3 · Vite · TypeScript · Element Plus · Pinia · ECharts |
| 工程化 | Maven · Docker Compose · CI |

## 快速开始

```bash
make run     # 一键启动，然后浏览器打开 http://localhost:8088
make stop    # 用完关掉，释放资源
```

打开后是统一登录页，顶部可选「商家 / 平台管理员 / 顾客」三种身份（演示账号点击即可填入）：

| 身份 | 默认账号 |
|---|---|
| 商家后台 | `admin-a / admin123`（租户 A）或 `admin-b / admin123`（租户 B） |
| 平台管理员 | `platform-admin / admin123` |
| 顾客商城 | `customer01 / admin123` |

详细启动方式、端口约定、环境要求见 [RUN.md](docs/RUN.md)。

## 文档

| 文档 | 说明 |
|------|------|
| [FILES.md](FILES.md) | 逐文件说明：每个文件 / 目录的作用 |
| [PROJECT.md](docs/PROJECT.md) | 架构设计、核心流程、数据模型 |
| [RUN.md](docs/RUN.md) | 运行手册：启动方式、端口、账号 |
| [TECHNICAL_QA.md](docs/TECHNICAL_QA.md) | 技术问答：核心设计的常见问题与解答 |
