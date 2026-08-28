-- 商品收藏表：保存“哪个顾客收藏了哪个商品“
CREATE TABLE IF NOT EXISTS product_favorite(
    id          BIGINT NOT NULL AUTO_INCREMENT COMMENT'收藏记录主键',
    tenant_id   BIGINT NOT NULL  COMMENT'顾客账号所属租户 ID',
    customer_id BIGINT NOT NULL  COMMENT'顾客 ID',
    product_id  BIGINT NOT NULL  COMMENT'被收藏的商品ID',
    created_at  DATETIME NOT NULL  DEFAULT CURRENT_TIMESTAMP COMMENT'收藏时间',

    PRIMARY KEY(id),

    -- 唯一索引：同一顾客不能重复收藏同一商品。
    UNIQUE KEY uk_customer_product (customer_id,product_id),
    -- 支持按顾客查询，并且按收藏时间倒序排列
    KEY idx_tenant_customer_created(tenant_id,customer_id,created_at,id),
    KEY idx_product_id(product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='顾客商品收藏';
