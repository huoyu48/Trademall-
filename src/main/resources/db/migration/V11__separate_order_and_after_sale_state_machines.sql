-- 订单只保留履约状态；退款、退货、物流和收货过程全部由 refund 售后单记录。
--
-- V10 已经在现有环境执行，因此本迁移从 V10 的字段结构继续演进，
-- 不能改写或复用已发布的 V10 版本号。

-- 先恢复旧“退款中”订单到发起售后前的履约状态，避免遗留退款中状态留在订单主表。
UPDATE orders o
LEFT JOIN refund r ON r.order_id = o.id
SET o.status = CASE r.original_order_status
    WHEN 'PAID' THEN 'PENDING_MERCHANT_CONFIRMATION'
    WHEN 'CONFIRMED' THEN 'PENDING_SHIPMENT'
    WHEN 'COMPLETED' THEN 'COMPLETED'
    ELSE 'COMPLETED'
END
WHERE o.status = 'REFUNDING';

UPDATE orders SET status = 'PENDING_MERCHANT_CONFIRMATION' WHERE status IN ('PAID', 'CREATED');
UPDATE orders SET status = 'PENDING_SHIPMENT' WHERE status = 'CONFIRMED';

-- 将 V10 的售后字段升级为当前业务命名；保留 return_approved_at 作为历史兼容字段。
ALTER TABLE refund
    CHANGE COLUMN after_sale_type refund_type VARCHAR(24) NOT NULL DEFAULT 'REFUND_ONLY',
    ADD COLUMN return_logistics_company VARCHAR(64) NULL AFTER refund_type,
    CHANGE COLUMN returned_at return_shipped_at DATETIME NULL,
    CHANGE COLUMN received_at merchant_received_at DATETIME NULL;

UPDATE refund
SET status = CASE status
    WHEN 'PENDING' THEN 'PENDING_REVIEW'
    WHEN 'APPROVED' THEN 'REFUNDING'
    ELSE status
END;

-- 历史已完成订单的售后按退货退款展示；其余按仅退款展示。
UPDATE refund r
JOIN orders o ON o.id = r.order_id
SET r.refund_type = CASE WHEN o.status = 'COMPLETED' THEN 'RETURN_AND_REFUND' ELSE 'REFUND_ONLY' END
WHERE r.refund_type = 'REFUND_ONLY';
