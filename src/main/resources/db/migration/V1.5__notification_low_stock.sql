-- 阶段6：通知表支持非订单类事件（如库存低水位告警）
-- 背景：V1.4 建表时 order_id 为 NOT NULL 且无默认值，导致 inventory.low-stock
-- 这类"与订单无关"的通知无法落库（Field 'order_id' doesn't have a default value），
-- 消费者持续抛异常并被重投。此处放开为可空，并补充按事件类型检索的索引。
ALTER TABLE notification
    MODIFY COLUMN order_id BIGINT NULL COMMENT '关联订单，非订单类事件为 NULL';

ALTER TABLE notification
    ADD COLUMN product_id BIGINT NULL COMMENT '关联商品，非商品类事件为 NULL' AFTER order_id;

ALTER TABLE notification
    ADD INDEX idx_tenant_event (tenant_id, event_type, created_at);
