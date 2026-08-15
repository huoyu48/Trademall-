-- 顾客表：顾客端登录与下单身份
CREATE TABLE IF NOT EXISTS customer (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id       BIGINT       NOT NULL COMMENT '所属租户',
    username        VARCHAR(64)  NOT NULL,
    password_hash   VARCHAR(128) NOT NULL,
    nickname        VARCHAR(64)  DEFAULT NULL,
    phone           VARCHAR(32)  DEFAULT NULL,
    status          TINYINT      DEFAULT 1 COMMENT '1=启用 0=禁用',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_username (tenant_id, username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='顾客';

-- 订单增加顾客维度（顾客端下单时回填）
ALTER TABLE orders
    ADD COLUMN customer_id BIGINT DEFAULT NULL COMMENT '下单顾客ID（顾客端）' AFTER customer_name;
