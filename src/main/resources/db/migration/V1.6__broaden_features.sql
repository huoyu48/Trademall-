-- 阶段6（续）：扩展业务广度，补齐"完善项目"所需的四大功能模块
-- 商品分类(category)、门店(store)、营销活动(promotion 满减/优惠券)、退款售后(refund)
-- 并给 product 表补充 category_id / store_id 两个维度字段。

CREATE TABLE category (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL,
    category_code VARCHAR(32)  NOT NULL,
    category_name VARCHAR(64)  NOT NULL,
    parent_id     BIGINT       NOT NULL DEFAULT 0,
    sort          INT          NOT NULL DEFAULT 0,
    status        TINYINT      NOT NULL DEFAULT 1,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_code (tenant_id, category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类';

CREATE TABLE store (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL,
    store_code  VARCHAR(32)  NOT NULL,
    store_name  VARCHAR(64)  NOT NULL,
    province    VARCHAR(32)  DEFAULT NULL,
    city        VARCHAR(32)  DEFAULT NULL,
    address     VARCHAR(255) DEFAULT NULL,
    status      TINYINT      NOT NULL DEFAULT 1,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_code (tenant_id, store_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店';

CREATE TABLE promotion (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,
    promo_code          VARCHAR(32)  NOT NULL,
    promo_name          VARCHAR(64)  NOT NULL,
    promo_type          VARCHAR(20)  NOT NULL COMMENT 'FULL_REDUCTION=满减, COUPON=优惠券',
    threshold_cent      BIGINT       NOT NULL DEFAULT 0 COMMENT '满减门槛（分），0 表示无门槛',
    discount_amount_cent BIGINT      NOT NULL DEFAULT 0 COMMENT '减免金额（分）',
    begin_at            DATETIME     DEFAULT NULL,
    end_at              DATETIME     DEFAULT NULL,
    status              TINYINT      NOT NULL DEFAULT 1,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_code (tenant_id, promo_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='营销活动';

CREATE TABLE refund (
    id                 BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id          BIGINT       NOT NULL,
    refund_no          VARCHAR(64)  NOT NULL,
    order_id           BIGINT       NOT NULL,
    order_no           VARCHAR(64)  DEFAULT NULL,
    reason             VARCHAR(255) DEFAULT NULL,
    refund_amount_cent BIGINT       DEFAULT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/REFUNDED',
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_refund_no (refund_no),
    KEY idx_tenant_order (tenant_id, order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款售后';

ALTER TABLE product
    ADD COLUMN category_id BIGINT DEFAULT NULL COMMENT '商品分类' AFTER product_name,
    ADD COLUMN store_id    BIGINT DEFAULT NULL COMMENT '所属门店' AFTER category_id;
