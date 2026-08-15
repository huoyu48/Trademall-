-- 阶段2：商品与库存
CREATE TABLE IF NOT EXISTS product (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id        BIGINT       NOT NULL,
    product_code     VARCHAR(64)  NOT NULL,
    product_name     VARCHAR(128) NOT NULL,
    unit_price_cent  BIGINT       NOT NULL DEFAULT 0 COMMENT '单价（分）',
    status           TINYINT      NOT NULL DEFAULT 1 COMMENT '1-在售 0-停用',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_product_code (tenant_id, product_code),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS inventory (
    id                 BIGINT   NOT NULL AUTO_INCREMENT,
    tenant_id          BIGINT   NOT NULL,
    product_id         BIGINT   NOT NULL,
    physical_quantity  BIGINT   NOT NULL DEFAULT 0,
    reserved_quantity  BIGINT   NOT NULL DEFAULT 0,
    version            BIGINT   NOT NULL DEFAULT 0 COMMENT '乐观锁',
    created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_product (tenant_id, product_id),
    KEY idx_product_id (product_id),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS inventory_adjustment (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id      BIGINT       NOT NULL,
    product_id     BIGINT       NOT NULL,
    change_quantity BIGINT      NOT NULL,
    reason         VARCHAR(255) NULL,
    operator_id    BIGINT       NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_tenant_id (tenant_id),
    KEY idx_product_id (product_id),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
