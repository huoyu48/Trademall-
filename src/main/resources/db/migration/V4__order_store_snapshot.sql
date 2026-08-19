-- 订单保存门店快照：顾客下单后仍能知道商品属于哪家店铺，并可从订单页联系商家。
ALTER TABLE orders
    ADD COLUMN store_id BIGINT DEFAULT NULL COMMENT '下单门店ID快照' AFTER customer_id,
    ADD COLUMN store_name_snapshot VARCHAR(64) DEFAULT NULL COMMENT '下单门店名称快照' AFTER store_id;

-- 为历史订单回填门店；历史订单若包含多个门店商品，则只在所有商品同店时回填。
UPDATE orders o
JOIN (
    SELECT oi.order_id, MIN(p.store_id) AS store_id,
           MIN(s.store_name) AS store_name,
           COUNT(DISTINCT p.store_id) AS store_count
    FROM order_item oi
    JOIN product p ON p.id = oi.product_id
    LEFT JOIN store s ON s.id = p.store_id
    GROUP BY oi.order_id
) snapshot ON snapshot.order_id = o.id
SET o.store_id = snapshot.store_id,
    o.store_name_snapshot = snapshot.store_name
WHERE snapshot.store_count = 1
  AND snapshot.store_id IS NOT NULL;
