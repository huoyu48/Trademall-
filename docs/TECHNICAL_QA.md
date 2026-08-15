# OrderFlow — 技术问答

> 本文档整理核心设计的技术要点与常见问题解答，供深入理解项目的实现细节。

## 一句话定位

OrderFlow 是一个**多租户订单履约平台**：覆盖高并发下单防超卖、订单状态机、异步可靠通知（消息重试 + 死信兜底）、多租户数据隔离等真实后端难点，前端配套 Vue3 + ECharts 数据看板。

## 技术栈

- 后端：Spring Boot 3.3 / Java 17、MyBatis-Plus 3.5、MySQL 8、Redis、RabbitMQ、Spring Security + JWT
- 前端：Vue 3 + Vite + TypeScript + Element Plus + Pinia + ECharts
- 工程化：Flyway 数据迁移、Docker 容器化、设计系统（CSS 变量 + Element Plus 主题覆写）

## 核心设计要点

1. **多租户数据隔离**：MyBatis-Plus 多租户拦截器在 SQL 执行前自动注入 `tenant_id` 条件，业务代码零侵入；对消费者线程、统计报表等无租户上下文的场景用 `@InterceptorIgnore` 显式豁免。
2. **高并发下单防超卖**：基于 Redis `SET NX` 的分布式锁锁定**商品维度**，扣减库存前获取锁，串行化同一商品的并发扣减；数据库层再用 `UPDATE ... WHERE stock >= 购买量` 做原子兜底。
3. **订单状态机**：状态流转集中校验，非法流转直接拒绝；创建订单带**幂等键（Idempotency-Key）**防止重复提交。
4. **异步可靠通知 + 死信兜底**：下单后发 RabbitMQ 事件，消费者**幂等**落库通知；消费失败按 `adviceChain` 重试 3 次，仍失败则拒收进入**死信队列 DLQ**，死信消费者归档到 `notification_failure` 表，保证最终一致性与可观测。
5. **订单超时自动取消**：`@EnableScheduling` 定时扫描超时未支付订单并取消，释放库存。
6. **安全**：Spring Security + JWT，未认证返回 **401**（而非 403）便于前端识别跳转。

## 常见问题解答

**Q：多租户怎么实现的？为什么不用独立库 / 独立 schema？**
A：采用"共享库 + `tenant_id` 行级隔离"，通过 MyBatis-Plus 多租户拦截器**无侵入**地给每条 SQL 拼接 `tenant_id` 条件。相比独立库，成本低、运维简单；相比独立 schema，跨租户统计方便。风险是拦截器遗漏导致越权，所以用 `@InterceptorIgnore` 明确豁免 MQ 线程 / 报表等无租户上下文场景，并配合测试覆盖。

**Q：怎么防止超卖？**
A：两层防护。① 应用层 Redis 分布式锁（`SET NX EX`）锁商品粒度，串行化同一商品的扣库存；② 数据库层在 `UPDATE` 时带 `WHERE stock >= 购买量` 条件，保证即使并发穿透也有原子约束。锁释放用唯一 value + Lua（或 Redisson）防止误删，并设置锁超时兜底。

**Q：消息队列为什么用 RabbitMQ？消息丢失 / 重复怎么办？**
A：订单域需要可靠投递、失败重试、死信、延迟，RabbitMQ 的 DLX / 重试机制更顺手（埋点流场景我们另一个项目 InsightFlow 用了 Kafka）。防丢失：生产者 confirm、消费者手动 ack；防重复：事件带 `eventId`，消费侧幂等去重；失败：重试 + 死信归档，运营可后台重发。

**Q：死信队列消费失败会怎样？**
A：死信消费者只是"兜底归档"，自身 `try-catch` 包裹、不再抛异常，避免死循环；归档后运营可在后台看到失败事件并重发。

**Q：状态机相比直接改状态字段有什么好处？**
A：集中校验合法流转，杜绝非法状态（如已取消 → 已支付），易测试、易扩展（加"退款中"只需加一个节点与转移规则）。

**Q：如果 Redis 锁过期但业务还没执行完怎么办？**
A：经典问题。生产可用 Redisson 的 watch dog 自动续期；或把锁超时设得大于业务最大耗时，并在释放时校验持有者。也可降级为"库存字段原子扣减"兜底，保证不超卖。

## 可继续优化的方向

- 用 Redisson 替代手写锁，支持可重入与看门狗续期
- 库存扣减引入 Redis 预扣 + MySQL 落库双写，提升读性能
- 通知消费幂等可用 Redis / 布隆过滤器快速去重
- 引入 Seata 做"扣库存 + 创建订单"的分布式事务（AT 模式），并讨论与最终一致性的取舍

## 演示与验证

- 启动步骤见 `RUN.md`
- 默认账号：`admin-a / admin123`（租户 A）、`admin-b / admin123`（租户 B）
- 关键接口：`POST /api/auth/login`、`GET /api/orders`、`GET /api/orders/stats`、`GET /api/health`
- 前端看板：`http://localhost:8088`（仪表盘、商品、库存、订单管理）
