-- 订单事件采用 Outbox：订单事务提交时先可靠落库，由后台任务异步投递 RabbitMQ。
CREATE TABLE IF NOT EXISTS outbox_event (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    event_id      VARCHAR(64)  NOT NULL,
    tenant_id     BIGINT       NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id  BIGINT       NOT NULL,
    event_type    VARCHAR(64)  NOT NULL,
    payload       TEXT         NOT NULL,
    status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SENDING/SENT/FAILED',
    retry_count   INT          NOT NULL DEFAULT 0,
    next_retry_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error    VARCHAR(1024) NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    sent_at       DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_outbox_event_id (event_id),
    KEY idx_outbox_publish (status, next_retry_at, id),
    KEY idx_outbox_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='可靠事件发件箱';

-- 消费端按 event_id 去重，允许 Outbox 在网络异常时安全重投。
ALTER TABLE notification
    ADD COLUMN event_id VARCHAR(64) NULL AFTER id,
    ADD UNIQUE KEY uk_notification_event_id (event_id);
