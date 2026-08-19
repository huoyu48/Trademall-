# OrderFlow

> 一个**多租户订单履约平台**：一套系统服务多个商家，商家管理商品与订单、顾客在线下单、平台全局治理，覆盖下单、扣库存、订单流转、异步通知的完整链路。

## 核心特性

- **多租户数据隔离**
  基于 MyBatis-Plus 多租户拦截器在 SQL 层自动注入 `tenant_id`，租户之间数据彻底隔离，业务代码零侵入。

- **高并发防超卖**
  Redis 分布式锁只串行化同一商家的同一商品；多商品订单按商品 ID 顺序加锁避免死锁。数据库条件更新作为最终防线，扣库存与建订单在同一事务边界内，要么都成、要么都回滚。

- **订单状态机**
  顾客订单生命周期 `PENDING_PAYMENT → PAID → CONFIRMED → SHIPPED → COMPLETED`，终态 `CANCELLED`。状态只能沿允许的边流转，非法跳转被拒绝，流转全程可审计。

- **模拟扫码支付**
  顾客下单后生成项目内收银台二维码；手机扫码打开独立的模拟付款页，确认后服务端以条件更新把订单改为已付款。支付流水独立落库，支持顾客取消待付款订单与超时关闭，均会回补库存并关闭付款码。

- **异步解耦 + 可靠通知**
  下单事务内写 Outbox 事件，后台收到 RabbitMQ Broker 确认后标记已投递；消费失败重试 3 次，仍失败则进入死信队列归档，消费端按事件 ID 去重。

- **订单超时自动取消**
  定时扫描超时未支付订单，自动取消并回补库存。

- **顾客—商家实时咨询**
  顾客从商品详情发起咨询，消息先持久化到 MySQL，再在事务提交后通过 WebSocket + STOMP 推送给顾客和对应商家；支持会话列表、未读数、历史消息分页与离线补拉，并按顾客身份和商家租户严格校验会话归属。

- **可观测性**
  `/api/health` 端点逐项探活 MySQL / Redis / RabbitMQ；通知与死信结构化落库，便于排查。

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Spring Boot 3.3.5 · Java 17 · MyBatis-Plus · Spring Security + JWT · WebSocket/STOMP · Flyway |
| 存储 / 中间件 | MySQL · Redis（分布式锁）· RabbitMQ（异步 + 死信） |
| 前端 | Vue 3 · Vite · TypeScript · Element Plus · Pinia · ECharts |
| 工程化 | Maven · Docker Compose · CI |

## 快速开始

```bash
make run     # 一键启动，然后浏览器打开 http://localhost:8088
make stop    # 用完关掉，释放资源
make stress  # 服务运行后，验证并发下单库存守恒
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
| [PAYMENT.md](docs/PAYMENT.md) | 模拟支付流程与支付专题面试题 |
| [VERIFICATION.md](docs/VERIFICATION.md) | 本地测试与并发/实时消息验证记录 |
