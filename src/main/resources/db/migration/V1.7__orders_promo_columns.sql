-- 阶段7：为 orders 表补充营销字段，支撑下单时记录使用的促销编码与减免金额
ALTER TABLE orders
    ADD COLUMN promo_code           VARCHAR(32) DEFAULT NULL COMMENT '使用的营销活动编码' AFTER total_amount_cent,
    ADD COLUMN discount_amount_cent BIGINT      DEFAULT 0     COMMENT '优惠减免金额（分）' AFTER promo_code;
