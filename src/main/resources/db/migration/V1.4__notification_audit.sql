-- 阶段5：通知、失败记录、审计日志
CREATE TABLE IF NOT EXISTS notification (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id  BIGINT       NOT NULL,
    order_id   BIGINT       NOT NULL,
    event_type VARCHAR(32)  NOT NULL,
    channel    VARCHAR(32)  NOT NULL DEFAULT 'INTERNAL',
    content    VARCHAR(512) NULL,
    status     TINYINT      NOT NULL DEFAULT 0 COMMENT '0-待发送 1-已发送 2-失败',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_tenant_id (tenant_id),
    KEY idx_order_id (order_id),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notification_failure (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id   BIGINT       NOT NULL,
    event_id    VARCHAR(64)  NOT NULL,
    event_type  VARCHAR(32)  NOT NULL,
    order_id    BIGINT       NULL,
    error_msg   VARCHAR(1024) NULL,
    retry_count INT          NOT NULL DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0-待处理 1-已重投 2-已丢弃',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_event_id (event_id),
    KEY idx_tenant_id (tenant_id),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS audit_log (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id      BIGINT       NOT NULL,
    actor_id       BIGINT       NULL,
    action         VARCHAR(64)  NOT NULL,
    target_type    VARCHAR(64)  NOT NULL,
    target_id      VARCHAR(64)  NULL,
    before_snapshot TEXT        NULL,
    after_snapshot  TEXT        NULL,
    request_id     VARCHAR(64)  NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_tenant_id (tenant_id),
    KEY idx_target (target_type, target_id),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
