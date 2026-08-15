-- 阶段3：订单与状态机
CREATE TABLE IF NOT EXISTS orders (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id          BIGINT       NOT NULL,
    order_no           VARCHAR(64)  NOT NULL,
    customer_name      VARCHAR(128) NOT NULL,
    status             VARCHAR(32)  NOT NULL DEFAULT 'CREATED',
    total_amount_cent  BIGINT       NOT NULL DEFAULT 0 COMMENT '订单总额（分）',
    idempotency_key    VARCHAR(64)  NULL,
    created_by         BIGINT       NULL,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_order_no (tenant_id, order_no),
    UNIQUE KEY uk_tenant_idempotency (tenant_id, idempotency_key),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_item (
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    order_id               BIGINT       NOT NULL,
    product_id             BIGINT       NOT NULL,
    product_code_snapshot  VARCHAR(64)  NOT NULL,
    product_name_snapshot  VARCHAR(128) NOT NULL,
    unit_price_cent        BIGINT       NOT NULL,
    quantity               INT          NOT NULL,
    line_amount_cent       BIGINT       NOT NULL,
    PRIMARY KEY (id),
    KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_status_history (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id    BIGINT       NOT NULL,
    order_id     BIGINT       NOT NULL,
    from_status  VARCHAR(32)  NULL,
    to_status    VARCHAR(32)  NOT NULL,
    operator_id  BIGINT       NULL,
    remark       VARCHAR(255) NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_tenant_order (tenant_id, order_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
